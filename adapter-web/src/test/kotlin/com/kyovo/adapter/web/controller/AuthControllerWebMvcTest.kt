package com.kyovo.adapter.web.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.kyovo.adapter.web.dto.LoginRequest
import com.kyovo.adapter.web.dto.RegisterRequest
import com.kyovo.adapter.web.security.JwtService
import com.kyovo.adapter.web.security.TokenBlacklistService
import com.kyovo.domain.exception.EmailAlreadyUsedException
import com.kyovo.domain.model.*
import com.kyovo.domain.port.primary.UserUseCase
import com.kyovo.domain.port.secondary.PasswordHashPort
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
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
    private lateinit var userUseCase: UserUseCase

    @MockitoBean
    private lateinit var passwordHashPort: PasswordHashPort

    @MockitoBean
    private lateinit var jwtService: JwtService

    @MockitoBean
    private lateinit var tokenBlacklistService: TokenBlacklistService

    private val userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    private val savedUser =
        User(UserId(userId), UserName("Alice"), UserEmail("alice@example.com"), UserPassword("hashed"), UserRole.USER)

    @Test
    fun `POST api-auth-register returns 201 with user info`()
    {
        whenever(userUseCase.save(any())).thenReturn(savedUser)

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
        whenever(userUseCase.save(any())).thenThrow(EmailAlreadyUsedException(UserEmail("alice@example.com")))

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
        whenever(userUseCase.findByEmail(UserEmail("alice@example.com"))).thenReturn(savedUser)
        whenever(passwordHashPort.matches("secret", UserPassword("hashed"))).thenReturn(true)
        whenever(jwtService.generateToken(UserId(userId), UserRole.USER)).thenReturn("jwt-token")

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
        whenever(userUseCase.findByEmail(any())).thenReturn(null)

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
        whenever(userUseCase.findByEmail(UserEmail("alice@example.com"))).thenReturn(savedUser)
        whenever(passwordHashPort.matches("wrong", UserPassword("hashed"))).thenReturn(false)

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
        val token = "valid-jwt-token"
        val expirationTime = System.currentTimeMillis() + 3600000

        whenever(jwtService.extractExpirationTime(token)).thenReturn(expirationTime)
        whenever(jwtService.validateToken(token)).thenReturn(true)

        mockMvc.post("/api/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
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
        val token = "invalid-jwt-token"

        whenever(jwtService.extractExpirationTime(token)).thenReturn(null)
        whenever(jwtService.validateToken(token)).thenReturn(false)

        mockMvc.post("/api/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
            with(csrf())
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
