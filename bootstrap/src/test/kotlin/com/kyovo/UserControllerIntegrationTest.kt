package com.kyovo

import com.kyovo.config.TestTimeProviderConfig
import com.kyovo.infrastructure.api.dto.LoginRequest
import com.kyovo.infrastructure.api.dto.RegisterRequest
import com.kyovo.infrastructure.api.dto.UpdateUserRequest
import com.kyovo.infrastructure.persistence.entity.UserEntity
import com.kyovo.infrastructure.persistence.repository.UserJpaRepository
import com.kyovo.infrastructure.provider.MutableTimeProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.*
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@SpringBootTest
@Import(TestTimeProviderConfig::class)
@AutoConfigureMockMvc
class UserControllerIntegrationTest
{
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var adminToken: String
    private lateinit var aliceToken: String
    private lateinit var aliceId: String

    @Autowired
    lateinit var timeProvider: MutableTimeProvider

    @BeforeEach
    fun setUp()
    {
        timeProvider.setNow(OffsetDateTime.of(LocalDateTime.of(2026, 2, 1, 10, 0), ZoneOffset.UTC))

        userJpaRepository.deleteAll()

        userJpaRepository.save(
            UserEntity(
                id = UUID.randomUUID(),
                name = "Admin",
                email = "admin@test.com",
                password = passwordEncoder.encode("admin123")!!,
                role = "ADMIN",
                registeredAt = timeProvider.now(),
                status = "CREATED",
                since = timeProvider.now(),
            )
        )
        adminToken = loginAndGetToken("admin@test.com", "admin123")

        val registerResult = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Alice", "alice@test.com", "alice123"))
        }.andReturn()
        aliceId = objectMapper.readTree(registerResult.response.contentAsString)["id"].asString()
        aliceToken = loginAndGetToken("alice@test.com", "alice123")
    }

    private fun loginAndGetToken(email: String, password: String): String
    {
        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest(email, password))
        }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["token"].asString()
    }

    @Test
    fun `GET api-users returns 200 with all users when authenticated as ADMIN`()
    {
        mockMvc.get("/api/users") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$") { isArray() }
        }
    }

    @Test
    fun `GET api-users returns 403 when authenticated as USER`()
    {
        mockMvc.get("/api/users") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET api-users-id returns 200 when authenticated as ADMIN`()
    {
        mockMvc.get("/api/users/$aliceId") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(aliceId) }
            jsonPath("$.name") { value("Alice") }
            jsonPath("$.status_info.status") { value("CREATED") }
            jsonPath("$.status_info.since") { value("2026-02-01T10:00:00Z") }
        }
    }

    @Test
    fun `GET api-users-id returns 404 for a non-existent identifier`()
    {
        mockMvc.get("/api/users/${UUID.randomUUID()}") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `GET api-users-id returns 500 when user role is invalid`()
    {
        val savedUser = userJpaRepository.save(
            UserEntity(
                id = UUID.randomUUID(),
                name = "Alice",
                email = "alice@example.com",
                password = passwordEncoder.encode("password")!!,
                role = "INVALID_ROLE",
                registeredAt = timeProvider.now(),
                status = "CREATED",
                since = timeProvider.now(),
            )
        )

        mockMvc.get("/api/users/${savedUser.id}") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isInternalServerError() }
        }
    }

    @Test
    fun `GET api-users-id returns 500 when user status is invalid`()
    {
        val savedUser = userJpaRepository.save(
            UserEntity(
                id = UUID.randomUUID(),
                name = "Alice",
                email = "alice@example.com",
                password = passwordEncoder.encode("password")!!,
                role = "USER",
                registeredAt = timeProvider.now(),
                status = "INVALID_STATUS",
                since = timeProvider.now(),
            )
        )

        mockMvc.get("/api/users/${savedUser.id}") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isInternalServerError() }
        }
    }

    @Test
    fun `PUT api-users-id returns 200 when user updates own account`()
    {
        mockMvc.put("/api/users/$aliceId") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateUserRequest("Alice Updated", null, null))
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Alice Updated") }
        }

        val updated = userJpaRepository.findByEmail("alice@test.com")
        assertThat(updated?.name).isEqualTo("Alice Updated")
    }

    @Test
    fun `PUT api-users-id returns 403 when user updates another account`()
    {
        val otherResult = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Bob", "bob@test.com", "bob123"))
        }.andReturn()
        val bobId = objectMapper.readTree(otherResult.response.contentAsString)["id"].asString()

        mockMvc.put("/api/users/$bobId") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateUserRequest("Hacked", null, null))
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `DELETE api-users-id returns 204 when user deletes own account`()
    {
        mockMvc.delete("/api/users/$aliceId") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isNoContent() }
        }

        assertThat(userJpaRepository.findById(UUID.fromString(aliceId))).isEmpty
    }

    @Test
    fun `DELETE api-users-id returns 403 when user deletes another account`()
    {
        val otherResult = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Bob", "bob@test.com", "bob123"))
        }.andReturn()
        val bobId = objectMapper.readTree(otherResult.response.contentAsString)["id"].asString()

        mockMvc.delete("/api/users/$bobId") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isForbidden() }
        }
    }
}
