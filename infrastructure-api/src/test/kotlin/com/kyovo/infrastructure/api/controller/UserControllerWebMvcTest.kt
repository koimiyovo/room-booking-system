package com.kyovo.infrastructure.api.controller

import com.kyovo.domain.exception.UserNotFoundException
import com.kyovo.domain.model.user.*
import com.kyovo.domain.port.primary.UserUseCase
import com.kyovo.domain.port.secondary.PasswordHashPort
import com.kyovo.infrastructure.api.dto.UpdateUserRequest
import com.kyovo.infrastructure.api.security.JwtService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import java.util.*

@WebMvcTest(UserController::class)
class UserControllerWebMvcTest
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

    private val userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    private val user =
        User(
            UserId(userId),
            UserName("Alice"),
            UserEmail("alice@example.com"),
            UserPassword("hashed"),
            UserRole.USER,
            UserRegistrationDate(OffsetDateTime.now()),
            UserStatusInfo(status = UserStatus.CREATED, since = UserStatusInfoDate(OffsetDateTime.now()))
        )

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `GET api-users returns 200 with the list of users`()
    {
        whenever(userUseCase.findAll()).thenReturn(listOf(user))

        mockMvc.get("/api/users")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(userId.toString()) }
                jsonPath("$[0].name") { value("Alice") }
                jsonPath("$[0].email") { value("alice@example.com") }
                jsonPath("$[0].role") { value("USER") }
            }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `GET api-users returns 200 with an empty list`()
    {
        whenever(userUseCase.findAll()).thenReturn(emptyList())

        mockMvc.get("/api/users")
            .andExpect {
                status { isOk() }
                content { json("[]") }
            }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `GET api-users-id returns 200 with the user when it exists`()
    {
        whenever(userUseCase.findById(UserId(userId))).thenReturn(user)

        mockMvc.get("/api/users/$userId")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(userId.toString()) }
                jsonPath("$.name") { value("Alice") }
                jsonPath("$.email") { value("alice@example.com") }
            }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `GET api-users-id returns 404 when the user does not exist`()
    {
        whenever(userUseCase.findById(UserId(userId))).thenReturn(null)

        mockMvc.get("/api/users/$userId")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = ["USER"])
    fun `PUT api-users-id returns 200 when user updates own account`()
    {
        val request = UpdateUserRequest("Alice Updated", null, null)
        val updated = user.copy(name = UserName("Alice Updated"))
        whenever(userUseCase.update(any(), any())).thenReturn(updated)
        whenever(passwordHashPort.hash(any())).thenReturn(UserPassword("hashed"))

        mockMvc.put("/api/users/$userId") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(csrf())
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Alice Updated") }
        }
    }

    @Test
    @WithMockUser(username = "99999999-9999-9999-9999-999999999999", roles = ["USER"])
    fun `PUT api-users-id returns 403 when user updates another account`()
    {
        mockMvc.put("/api/users/$userId") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateUserRequest("X", null, null))
            with(csrf())
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = ["USER"])
    fun `DELETE api-users-id returns 204 when user deletes own account`()
    {
        mockMvc.delete("/api/users/$userId") {
            with(csrf())
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `DELETE api-users-id returns 404 when user does not exist`()
    {
        whenever(userUseCase.delete(UserId(userId))).thenThrow(UserNotFoundException(UserId(userId)))

        mockMvc.delete("/api/users/$userId") {
            with(csrf())
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @WithMockUser(username = "99999999-9999-9999-9999-999999999999", roles = ["USER"])
    fun `DELETE api-users-id returns 403 when user deletes another account`()
    {
        mockMvc.delete("/api/users/$userId") {
            with(csrf())
        }.andExpect {
            status { isForbidden() }
        }
    }
}
