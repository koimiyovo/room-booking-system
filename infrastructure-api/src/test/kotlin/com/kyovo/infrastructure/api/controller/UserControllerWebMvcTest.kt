package com.kyovo.infrastructure.api.controller

import com.kyovo.domain.exception.AccountNotOwnedByUserException
import com.kyovo.domain.exception.InvalidStatusTransitionException
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
import org.springframework.test.web.servlet.post
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

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = ["USER"])
    fun `POST api-users-id-validate returns 200 when user validates own account`()
    {
        val activeUser = user.copy(
            statusInfo = UserStatusInfo(status = UserStatus.ACTIVE, since = UserStatusInfoDate(OffsetDateTime.now()))
        )
        whenever(userUseCase.validate(any(), any(), any())).thenReturn(activeUser)

        mockMvc.post("/api/users/$userId/validate") {
            with(csrf())
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(userId.toString()) }
            jsonPath("$.status_info.status") { value("ACTIVE") }
        }
    }

    @Test
    @WithMockUser(username = "99999999-9999-9999-9999-999999999999", roles = ["USER"])
    fun `POST api-users-id-validate returns 403 when non-admin user validates another account`()
    {
        whenever(userUseCase.validate(any(), any(), any())).thenThrow(AccountNotOwnedByUserException())

        mockMvc.post("/api/users/$userId/validate") {
            with(csrf())
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = ["USER"])
    fun `POST api-users-id-validate returns 409 when status transition is invalid`()
    {
        whenever(userUseCase.validate(any(), any(), any()))
            .thenThrow(InvalidStatusTransitionException(UserStatus.ACTIVE, UserStatus.ACTIVE))

        mockMvc.post("/api/users/$userId/validate") {
            with(csrf())
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `POST api-users-id-validate returns 401 when unauthenticated`()
    {
        mockMvc.post("/api/users/$userId/validate") {
            with(csrf())
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `POST api-users-id-deactivate returns 200 when admin deactivates an active account`()
    {
        val inactiveUser = user.copy(
            statusInfo = UserStatusInfo(status = UserStatus.INACTIVE, since = UserStatusInfoDate(OffsetDateTime.now()))
        )
        whenever(userUseCase.deactivate(UserId(userId))).thenReturn(inactiveUser)

        mockMvc.post("/api/users/$userId/deactivate") {
            with(csrf())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status_info.status") { value("INACTIVE") }
        }
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `POST api-users-id-deactivate returns 403 when called by a non-admin user`()
    {
        mockMvc.post("/api/users/$userId/deactivate") {
            with(csrf())
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `POST api-users-id-deactivate returns 404 when user does not exist`()
    {
        whenever(userUseCase.deactivate(UserId(userId))).thenThrow(UserNotFoundException(UserId(userId)))

        mockMvc.post("/api/users/$userId/deactivate") {
            with(csrf())
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `POST api-users-id-deactivate returns 409 when status transition is invalid`()
    {
        whenever(userUseCase.deactivate(UserId(userId)))
            .thenThrow(InvalidStatusTransitionException(UserStatus.CREATED, UserStatus.INACTIVE))

        mockMvc.post("/api/users/$userId/deactivate") {
            with(csrf())
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `POST api-users-id-deactivate returns 401 when unauthenticated`()
    {
        mockMvc.post("/api/users/$userId/deactivate") {
            with(csrf())
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `POST api-users-id-reactivate returns 200 when admin reactivates an inactive account`()
    {
        val activeUser = user.copy(
            statusInfo = UserStatusInfo(status = UserStatus.ACTIVE, since = UserStatusInfoDate(OffsetDateTime.now()))
        )
        whenever(userUseCase.reactivate(UserId(userId))).thenReturn(activeUser)

        mockMvc.post("/api/users/$userId/reactivate") {
            with(csrf())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status_info.status") { value("ACTIVE") }
        }
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `POST api-users-id-reactivate returns 403 when called by a non-admin user`()
    {
        mockMvc.post("/api/users/$userId/reactivate") {
            with(csrf())
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `POST api-users-id-reactivate returns 404 when user does not exist`()
    {
        whenever(userUseCase.reactivate(UserId(userId))).thenThrow(UserNotFoundException(UserId(userId)))

        mockMvc.post("/api/users/$userId/reactivate") {
            with(csrf())
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `POST api-users-id-reactivate returns 409 when status transition is invalid`()
    {
        whenever(userUseCase.reactivate(UserId(userId)))
            .thenThrow(InvalidStatusTransitionException(UserStatus.ACTIVE, UserStatus.ACTIVE))

        mockMvc.post("/api/users/$userId/reactivate") {
            with(csrf())
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `POST api-users-id-reactivate returns 401 when unauthenticated`()
    {
        mockMvc.post("/api/users/$userId/reactivate") {
            with(csrf())
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
