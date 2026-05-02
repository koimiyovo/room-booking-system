package com.kyovo

import com.kyovo.config.TestTimeProviderConfig
import com.kyovo.infrastructure.api.dto.LoginRequest
import com.kyovo.infrastructure.api.dto.RegisterRequest
import com.kyovo.infrastructure.api.dto.UpdateUserRequest
import com.kyovo.infrastructure.persistence.entity.UserEntity
import com.kyovo.infrastructure.persistence.entity.UserStatusHistoryEntity
import com.kyovo.infrastructure.persistence.repository.UserJpaRepository
import com.kyovo.infrastructure.persistence.repository.UserStatusHistoryJpaRepository
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
    private lateinit var userStatusHistoryJpaRepository: UserStatusHistoryJpaRepository

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

        userStatusHistoryJpaRepository.deleteAll()
        userJpaRepository.deleteAll()

        val adminId = UUID.randomUUID()
        userJpaRepository.save(
            UserEntity(
                id = adminId,
                name = "Admin",
                email = "admin@test.com",
                password = passwordEncoder.encode("admin123")!!,
                role = "ADMIN",
                registeredAt = timeProvider.now(),
            )
        )
        userStatusHistoryJpaRepository.save(
            UserStatusHistoryEntity(
                id = UUID.randomUUID(),
                userId = adminId,
                status = "CREATED",
                since = timeProvider.now(),
                until = null,
                reason = null
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
            )
        )
        userStatusHistoryJpaRepository.save(
            UserStatusHistoryEntity(
                id = UUID.randomUUID(),
                userId = savedUser.id,
                status = "CREATED",
                since = timeProvider.now(),
                until = null,
                reason = null
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
            )
        )
        userStatusHistoryJpaRepository.save(
            UserStatusHistoryEntity(
                id = UUID.randomUUID(),
                userId = savedUser.id,
                status = "INVALID_STATUS",
                since = timeProvider.now(),
                until = null,
                reason = null
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

        val updated = userJpaRepository.findById(UUID.fromString(aliceId)).orElseThrow()
        assertThat(updated.name).isEqualTo("Alice Updated")
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

        val currentStatus = userStatusHistoryJpaRepository.findByUserIdAndUntilIsNull(UUID.fromString(aliceId))
        assertThat(currentStatus?.status).isEqualTo("DELETED")
    }

    @Test
    fun `DELETE api-users-id does not physically remove the user row`()
    {
        mockMvc.delete("/api/users/$aliceId") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isNoContent() }
        }

        assertThat(userJpaRepository.findById(UUID.fromString(aliceId))).isPresent
    }

    @Test
    fun `GET api-users includes deleted users`()
    {
        mockMvc.delete("/api/users/$aliceId") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect { status { isNoContent() } }

        mockMvc.get("/api/users") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.id == '$aliceId')].status_info.status") { value("DELETED") }
        }
    }

    @Test
    fun `GET api-users-id returns 404 for a deleted user`()
    {
        mockMvc.delete("/api/users/$aliceId") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect { status { isNoContent() } }

        mockMvc.get("/api/users/$aliceId") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `deleted user JWT is rejected with 401`()
    {
        mockMvc.delete("/api/users/$aliceId") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect { status { isNoContent() } }

        mockMvc.get("/api/users/$aliceId") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isUnauthorized() }
        }
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

    @Test
    fun `POST api-users-id-validate returns 200 and transitions status to ACTIVE`()
    {
        timeProvider.setNow(OffsetDateTime.of(LocalDateTime.of(2026, 3, 1, 12, 0), ZoneOffset.UTC))

        mockMvc.post("/api/users/$aliceId/validate") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(aliceId) }
            jsonPath("$.status_info.status") { value("ACTIVE") }
            jsonPath("$.status_info.since") { value("2026-03-01T12:00:00Z") }
        }

        val currentStatus = userStatusHistoryJpaRepository.findByUserIdAndUntilIsNull(UUID.fromString(aliceId))
        assertThat(currentStatus?.status).isEqualTo("ACTIVE")
    }

    @Test
    fun `POST api-users-id-validate records status history with closed CREATED entry and new ACTIVE entry`()
    {
        timeProvider.setNow(OffsetDateTime.of(LocalDateTime.of(2026, 3, 1, 12, 0), ZoneOffset.UTC))

        mockMvc.post("/api/users/$aliceId/validate") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isOk() }
        }

        val registrationTime = OffsetDateTime.of(LocalDateTime.of(2026, 2, 1, 10, 0), ZoneOffset.UTC)
        val validationTime = OffsetDateTime.of(LocalDateTime.of(2026, 3, 1, 12, 0), ZoneOffset.UTC)

        val history = userStatusHistoryJpaRepository.findAllByUserId(UUID.fromString(aliceId))
            .sortedBy { it.since }
        assertThat(history).hasSize(2)

        val createdEntry = history[0]
        assertThat(createdEntry.status).isEqualTo("CREATED")
        assertThat(createdEntry.since).isEqualTo(registrationTime)
        assertThat(createdEntry.until).isEqualTo(validationTime)

        val activeEntry = history[1]
        assertThat(activeEntry.status).isEqualTo("ACTIVE")
        assertThat(activeEntry.since).isEqualTo(validationTime)
        assertThat(activeEntry.until).isNull()
    }

    @Test
    fun `POST api-users-id-validate returns 403 when non-admin user validates another account`()
    {
        val otherResult = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Bob", "bob@test.com", "bob123"))
        }.andReturn()
        val bobId = objectMapper.readTree(otherResult.response.contentAsString)["id"].asString()

        mockMvc.post("/api/users/$bobId/validate") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `POST api-users-id-validate returns 409 when account is already active`()
    {
        timeProvider.setNow(OffsetDateTime.of(LocalDateTime.of(2026, 3, 1, 12, 0), ZoneOffset.UTC))
        mockMvc.post("/api/users/$aliceId/validate") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect { status { isOk() } }

        mockMvc.post("/api/users/$aliceId/validate") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `POST api-users-id-validate returns 200 when admin validates a user`()
    {
        timeProvider.setNow(OffsetDateTime.of(LocalDateTime.of(2026, 3, 1, 12, 0), ZoneOffset.UTC))

        mockMvc.post("/api/users/$aliceId/validate") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status_info.status") { value("ACTIVE") }
        }
    }

    @Test
    fun `POST api-users-id-deactivate returns 200 and transitions status to INACTIVE`()
    {
        timeProvider.setNow(OffsetDateTime.of(LocalDateTime.of(2026, 3, 1, 12, 0), ZoneOffset.UTC))
        mockMvc.post("/api/users/$aliceId/validate") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect { status { isOk() } }

        timeProvider.setNow(OffsetDateTime.of(LocalDateTime.of(2026, 4, 1, 9, 0), ZoneOffset.UTC))

        mockMvc.post("/api/users/$aliceId/deactivate") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status_info.status") { value("INACTIVE") }
            jsonPath("$.status_info.since") { value("2026-04-01T09:00:00Z") }
        }

        val currentStatus = userStatusHistoryJpaRepository.findByUserIdAndUntilIsNull(UUID.fromString(aliceId))
        assertThat(currentStatus?.status).isEqualTo("INACTIVE")
    }

    @Test
    fun `POST api-users-id-deactivate returns 403 when called by a non-admin user`()
    {
        mockMvc.post("/api/users/$aliceId/deactivate") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `POST api-users-id-deactivate returns 409 when account is not active`()
    {
        mockMvc.post("/api/users/$aliceId/deactivate") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `POST api-users-id-reactivate returns 200 and transitions status back to ACTIVE`()
    {
        timeProvider.setNow(OffsetDateTime.of(LocalDateTime.of(2026, 3, 1, 12, 0), ZoneOffset.UTC))
        mockMvc.post("/api/users/$aliceId/validate") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect { status { isOk() } }
        mockMvc.post("/api/users/$aliceId/deactivate") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect { status { isOk() } }

        timeProvider.setNow(OffsetDateTime.of(LocalDateTime.of(2026, 5, 1, 8, 0), ZoneOffset.UTC))

        mockMvc.post("/api/users/$aliceId/reactivate") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status_info.status") { value("ACTIVE") }
            jsonPath("$.status_info.since") { value("2026-05-01T08:00:00Z") }
        }

        val currentStatus = userStatusHistoryJpaRepository.findByUserIdAndUntilIsNull(UUID.fromString(aliceId))
        assertThat(currentStatus?.status).isEqualTo("ACTIVE")
    }

    @Test
    fun `POST api-users-id-reactivate returns 403 when called by a non-admin user`()
    {
        mockMvc.post("/api/users/$aliceId/reactivate") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `POST api-users-id-reactivate returns 409 when account is not inactive`()
    {
        mockMvc.post("/api/users/$aliceId/reactivate") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `deactivate and reactivate record full status history`()
    {
        val registrationTime = OffsetDateTime.of(LocalDateTime.of(2026, 2, 1, 10, 0), ZoneOffset.UTC)
        val validationTime = OffsetDateTime.of(LocalDateTime.of(2026, 3, 1, 12, 0), ZoneOffset.UTC)
        val deactivationTime = OffsetDateTime.of(LocalDateTime.of(2026, 4, 1, 9, 0), ZoneOffset.UTC)
        val reactivationTime = OffsetDateTime.of(LocalDateTime.of(2026, 5, 1, 8, 0), ZoneOffset.UTC)

        timeProvider.setNow(validationTime)
        mockMvc.post("/api/users/$aliceId/validate") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect { status { isOk() } }

        timeProvider.setNow(deactivationTime)
        mockMvc.post("/api/users/$aliceId/deactivate") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect { status { isOk() } }

        timeProvider.setNow(reactivationTime)
        mockMvc.post("/api/users/$aliceId/reactivate") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect { status { isOk() } }

        val history = userStatusHistoryJpaRepository.findAllByUserId(UUID.fromString(aliceId))
            .sortedBy { it.since }
        assertThat(history).hasSize(4)

        assertThat(history[0].status).isEqualTo("CREATED")
        assertThat(history[0].since).isEqualTo(registrationTime)
        assertThat(history[0].until).isEqualTo(validationTime)

        assertThat(history[1].status).isEqualTo("ACTIVE")
        assertThat(history[1].since).isEqualTo(validationTime)
        assertThat(history[1].until).isEqualTo(deactivationTime)

        assertThat(history[2].status).isEqualTo("INACTIVE")
        assertThat(history[2].since).isEqualTo(deactivationTime)
        assertThat(history[2].until).isEqualTo(reactivationTime)

        assertThat(history[3].status).isEqualTo("ACTIVE")
        assertThat(history[3].since).isEqualTo(reactivationTime)
        assertThat(history[3].until).isNull()
    }
}
