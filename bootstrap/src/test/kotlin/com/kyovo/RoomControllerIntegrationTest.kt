package com.kyovo

import com.kyovo.config.TestTimeProviderConfig
import com.kyovo.infrastructure.api.dto.CreateRoomRequest
import com.kyovo.infrastructure.api.dto.LoginRequest
import com.kyovo.infrastructure.persistence.entity.UserEntity
import com.kyovo.infrastructure.persistence.entity.UserStatusHistoryEntity
import com.kyovo.infrastructure.persistence.repository.RoomJpaRepository
import com.kyovo.infrastructure.persistence.repository.UserJpaRepository
import com.kyovo.infrastructure.persistence.repository.UserStatusHistoryJpaRepository
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import java.util.*

@SpringBootTest
@Import(TestTimeProviderConfig::class)
@AutoConfigureMockMvc
class RoomControllerIntegrationTest
{
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var roomJpaRepository: RoomJpaRepository

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var userStatusHistoryJpaRepository: UserStatusHistoryJpaRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var adminToken: String
    private lateinit var userToken: String

    @BeforeEach
    fun setUp()
    {
        roomJpaRepository.deleteAll()
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
                registeredAt = OffsetDateTime.now(),
            )
        )
        userStatusHistoryJpaRepository.save(
            UserStatusHistoryEntity(id = UUID.randomUUID(), userId = adminId, status = "CREATED", since = OffsetDateTime.now(), until = null, reason = null)
        )

        val userId = UUID.randomUUID()
        userJpaRepository.save(
            UserEntity(
                id = userId,
                name = "User",
                email = "user@test.com",
                password = passwordEncoder.encode("user123")!!,
                role = "USER",
                registeredAt = OffsetDateTime.now(),
            )
        )
        userStatusHistoryJpaRepository.save(
            UserStatusHistoryEntity(id = UUID.randomUUID(), userId = userId, status = "CREATED", since = OffsetDateTime.now(), until = null, reason = null)
        )

        adminToken = loginAndGetToken("admin@test.com", "admin123")
        userToken = loginAndGetToken("user@test.com", "user123")
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
    fun `GET api-rooms returns 200 with empty list when no rooms exist`()
    {
        mockMvc.get("/api/rooms") {
            header("Authorization", "Bearer $userToken")
        }.andExpect {
            status { isOk() }
            content { json("[]") }
        }
    }

    @Test
    fun `POST api-rooms returns 201 with the created room`()
    {
        mockMvc.post("/api/rooms") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateRoomRequest("Salle Conférence", 25))
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNotEmpty() }
            jsonPath("$.name") { value("Salle Conférence") }
            jsonPath("$.capacity") { value(25) }
        }
    }

    @Test
    fun `POST api-rooms returns 403 when authenticated as USER`()
    {
        mockMvc.post("/api/rooms") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateRoomRequest("Salle", 10))
            header("Authorization", "Bearer $userToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `POST then GET api-rooms returns the created room in the list`()
    {
        val postResult = mockMvc.post("/api/rooms") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateRoomRequest("Salle Réunion", 12))
            header("Authorization", "Bearer $adminToken")
        }.andReturn()

        val createdId = objectMapper.readTree(postResult.response.contentAsString)["id"].asString()

        mockMvc.get("/api/rooms") {
            header("Authorization", "Bearer $userToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(createdId) }
            jsonPath("$[0].name") { value("Salle Réunion") }
        }

        assertThat(roomJpaRepository.count()).isEqualTo(1)
    }

    @Test
    fun `POST then GET api-rooms-id returns the room by its identifier`()
    {
        val postResult = mockMvc.post("/api/rooms") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateRoomRequest("Salle Formation", 30))
            header("Authorization", "Bearer $adminToken")
        }.andReturn()

        val createdId = objectMapper.readTree(postResult.response.contentAsString)["id"].asString()

        mockMvc.get("/api/rooms/$createdId") {
            header("Authorization", "Bearer $userToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(createdId) }
            jsonPath("$.name") { value("Salle Formation") }
        }
    }

    @Test
    fun `GET api-rooms-id returns 404 for a non-existent identifier`()
    {
        mockMvc.get("/api/rooms/${UUID.randomUUID()}") {
            header("Authorization", "Bearer $userToken")
        }.andExpect {
            status { isNotFound() }
        }
    }
}
