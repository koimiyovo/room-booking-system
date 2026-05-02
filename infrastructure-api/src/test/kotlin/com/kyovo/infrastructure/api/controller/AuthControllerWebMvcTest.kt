package com.kyovo.infrastructure.api.controller

import com.kyovo.domain.exception.EmailAlreadyUsedException
import com.kyovo.domain.exception.InvalidCredentialsException
import com.kyovo.domain.model.user.*
import com.kyovo.domain.port.primary.AuthUseCase
import com.kyovo.domain.port.primary.UserUseCase
import com.kyovo.infrastructure.api.dto.LoginRequest
import com.kyovo.infrastructure.api.dto.RegisterRequest
import com.kyovo.infrastructure.api.security.AuthToken
import com.kyovo.infrastructure.api.security.JwtService
import com.kyovo.infrastructure.api.security.TokenBlacklistService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import java.util.*

@WebMvcTest(AuthController::class)
@WithMockUser
class AuthControllerWebMvcTest
{
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var authUseCase: AuthUseCase

    @MockitoBean
    private lateinit var userUseCase: UserUseCase

    @MockitoBean
    private lateinit var jwtService: JwtService

    @MockitoBean
    private lateinit var tokenBlacklistService: TokenBlacklistService

    private val userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    private val savedUser =
        User(
            UserId(userId),
            UserName("Alice"),
            UserEmail("alice@example.com"),
            UserPassword("hashed"),
            UserRole.USER,
            UserRegistrationDate(OffsetDateTime.now()),
            UserStatusInfo(status = UserStatus.CREATED, since = UserStatusInfoDate(OffsetDateTime.now()), reason = null)
        )

    @Test
    fun `POST api-auth-register returns 201 with user info`()
    {
        whenever(authUseCase.register(any())).thenReturn(savedUser)

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Alice", "alice@example.com", "secret"))
            with(csrf())
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { value(userId.toString()) }
            jsonPath("$.name") { value("Alice") }
            jsonPath("$.email") { value("alice@example.com") }
        }
    }

    @Test
    fun `POST api-auth-register returns 409 when email is already in use`()
    {
        whenever(authUseCase.register(any())).thenThrow(EmailAlreadyUsedException(UserEmail("alice@example.com")))

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Alice", "alice@example.com", "secret"))
            with(csrf())
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `POST api-auth-login returns 200 with a token`()
    {
        whenever(authUseCase.login(UserEmail("alice@example.com"), "secret")).thenReturn(savedUser)
        whenever(jwtService.generateToken(UserId(userId), UserRole.USER)).thenReturn(AuthToken("jwt-token"))

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("alice@example.com", "secret"))
            with(csrf())
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { value("jwt-token") }
        }
    }

    @Test
    fun `POST api-auth-login returns 401 when email is unknown`()
    {
        whenever(authUseCase.login(any(), any())).thenThrow(InvalidCredentialsException())

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("unknown@example.com", "secret"))
            with(csrf())
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `POST api-auth-login returns 401 when password is wrong`()
    {
        whenever(authUseCase.login(any(), any())).thenThrow(InvalidCredentialsException())

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("alice@example.com", "wrong"))
            with(csrf())
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    @WithMockUser
    fun `POST api-auth-logout returns 200 and revokes the token`()
    {
        val rawToken = "valid-jwt-token"
        val token = AuthToken(rawToken)
        val expirationTime = System.currentTimeMillis() + 3600000

        whenever(jwtService.extractExpirationTime(token)).thenReturn(expirationTime)
        whenever(jwtService.validateToken(token)).thenReturn(true)

        mockMvc.post("/api/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $rawToken")
            with(csrf())
        }.andExpect {
            status { isOk() }
        }

        verify(tokenBlacklistService).revokeToken(token, expirationTime)
    }

    @Test
    fun `POST api-auth-logout returns 401 when token is missing`()
    {
        mockMvc.post("/api/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            with(csrf())
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `POST api-auth-logout returns 401 when token is invalid`()
    {
        val token = AuthToken("invalid-jwt-token")

        whenever(jwtService.extractExpirationTime(token)).thenReturn(null)
        whenever(jwtService.validateToken(token)).thenReturn(false)

        mockMvc.post("/api/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${token.value}")
            with(csrf())
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
