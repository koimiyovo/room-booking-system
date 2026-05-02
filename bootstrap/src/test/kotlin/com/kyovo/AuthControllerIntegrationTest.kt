package com.kyovo

import com.kyovo.config.TestTimeProviderConfig
import com.kyovo.infrastructure.api.dto.LoginRequest
import com.kyovo.infrastructure.api.dto.RegisterRequest
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@SpringBootTest
@Import(TestTimeProviderConfig::class)
@AutoConfigureMockMvc
class AuthControllerIntegrationTest
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

    @Autowired
    lateinit var timeProvider: MutableTimeProvider

    @BeforeEach
    fun setUp()
    {
        timeProvider.setNow(OffsetDateTime.of(LocalDateTime.of(2026, 1, 1, 0, 0), ZoneOffset.UTC))
        userStatusHistoryJpaRepository.deleteAll()
        userJpaRepository.deleteAll()
    }

    @Test
    fun `POST api-auth-register creates a user and returns 201`()
    {
        timeProvider.setNow(OffsetDateTime.of(LocalDateTime.of(2026, 1, 2, 11, 30, 45), ZoneOffset.UTC))

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Alice", "alice@example.com", "password"))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNotEmpty() }
            jsonPath("$.name") { value("Alice") }
            jsonPath("$.email") { value("alice@example.com") }
            jsonPath("$.registered_at") { value("2026-01-02T11:30:45Z") }
        }

        assertThat(userJpaRepository.count()).isEqualTo(1)
    }

    @Test
    fun `POST api-auth-register returns 409 when email is already taken`()
    {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Alice", "alice@example.com", "password"))
        }.andExpect { status { isCreated() } }

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Alice2", "alice@example.com", "other"))
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `POST api-auth-login returns 200 with a valid token`()
    {
        val savedUser = userJpaRepository.save(
            UserEntity(
                id = UUID.randomUUID(),
                name = "Alice",
                email = "alice@example.com",
                password = passwordEncoder.encode("password")!!,
                role = "USER",
                registeredAt = OffsetDateTime.now(),
            )
        )
        userStatusHistoryJpaRepository.save(
            UserStatusHistoryEntity(id = UUID.randomUUID(), userId = savedUser.id, status = "CREATED", since = OffsetDateTime.now(), until = null, reason = null)
        )

        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("alice@example.com", "password"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { isNotEmpty() }
        }.andReturn()

        val token = objectMapper.readTree(result.response.contentAsString)["token"].asString()
        assertThat(token).isNotBlank()
    }

    @Test
    fun `POST api-auth-login returns 401 when password is wrong`()
    {
        val savedUser = userJpaRepository.save(
            UserEntity(
                id = UUID.randomUUID(),
                name = "Alice",
                email = "alice@example.com",
                password = passwordEncoder.encode("password")!!,
                role = "USER",
                registeredAt = OffsetDateTime.now(),
            )
        )
        userStatusHistoryJpaRepository.save(
            UserStatusHistoryEntity(id = UUID.randomUUID(), userId = savedUser.id, status = "CREATED", since = OffsetDateTime.now(), until = null, reason = null)
        )

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("alice@example.com", "wrong"))
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `POST api-auth-login returns 401 when email is unknown`()
    {
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("unknown@example.com", "password"))
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
