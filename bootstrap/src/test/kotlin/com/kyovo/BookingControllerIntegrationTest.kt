package com.kyovo

import com.kyovo.config.TestTimeProviderConfig
import com.kyovo.infrastructure.api.dto.*
import com.kyovo.infrastructure.persistence.entity.UserEntity
import com.kyovo.infrastructure.persistence.entity.UserStatusHistoryEntity
import com.kyovo.infrastructure.persistence.repository.BookingJpaRepository
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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@SpringBootTest
@Import(TestTimeProviderConfig::class)
@AutoConfigureMockMvc
class BookingControllerIntegrationTest
{
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var bookingJpaRepository: BookingJpaRepository

    @Autowired
    private lateinit var roomJpaRepository: RoomJpaRepository

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var userStatusHistoryJpaRepository: UserStatusHistoryJpaRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var adminToken: String
    private lateinit var aliceToken: String
    private lateinit var aliceId: UUID
    private lateinit var bobToken: String
    private lateinit var bobId: UUID

    @BeforeEach
    fun setUp()
    {
        bookingJpaRepository.deleteAll()
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
            UserStatusHistoryEntity(id = UUID.randomUUID(), userId = adminId, status = "CREATED", since = OffsetDateTime.now(), until = null)
        )
        adminToken = loginAndGetToken("admin@test.com", "admin123")

        val aliceResult = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Alice", "alice@test.com", "alice123"))
        }.andReturn()
        aliceId = UUID.fromString(objectMapper.readTree(aliceResult.response.contentAsString)["id"].asString())
        aliceToken = loginAndGetToken("alice@test.com", "alice123")

        val bobResult = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Bob", "bob@test.com", "bob123"))
        }.andReturn()
        bobId = UUID.fromString(objectMapper.readTree(bobResult.response.contentAsString)["id"].asString())
        bobToken = loginAndGetToken("bob@test.com", "bob123")
    }

    private fun loginAndGetToken(email: String, password: String): String
    {
        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest(email, password))
        }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["token"].asString()
    }

    private fun createRoom(name: String = "Conference Room", capacity: Int = 10): UUID
    {
        val result = mockMvc.post("/api/rooms") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateRoomRequest(name, capacity))
            header("Authorization", "Bearer $adminToken")
        }.andReturn()
        return UUID.fromString(objectMapper.readTree(result.response.contentAsString)["id"].asString())
    }

    @Test
    fun `POST then GET api-bookings returns the created booking`()
    {
        val roomId = createRoom()
        val request = CreateBookingRequest(
            roomId,
            aliceId,
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 3),
            5,
            "Projector needed"
        )

        val postResult = mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isCreated() }
            jsonPath("$.room_id") { value(roomId.toString()) }
            jsonPath("$.user_id") { value(aliceId.toString()) }
            jsonPath("$.special_requests") { value("Projector needed") }
        }.andReturn()

        val createdId = objectMapper.readTree(postResult.response.contentAsString)["id"].asString()

        mockMvc.get("/api/bookings/$createdId") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(createdId) }
            jsonPath("$.status") { value("CONFIRMED") }
        }

        assertThat(bookingJpaRepository.count()).isEqualTo(1)
    }

    @Test
    fun `POST api-bookings returns 404 when room does not exist`()
    {
        val request = CreateBookingRequest(
            UUID.randomUUID(),
            aliceId,
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 3),
            5,
            null
        )

        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST api-bookings returns 400 when capacity is exceeded`()
    {
        val roomId = createRoom(capacity = 5)
        val request =
            CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), 10, null)

        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST api-bookings returns 409 when dates overlap for the same room`()
    {
        val roomId = createRoom()
        val request = CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), 5, null)

        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $aliceToken")
        }.andExpect { status { isCreated() } }

        val overlapping =
            CreateBookingRequest(roomId, bobId, LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 7), 5, null)
        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(overlapping)
            header("Authorization", "Bearer $bobToken")
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `cancelled booking slot can be rebooked`()
    {
        val roomId = createRoom()
        val request = CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), 5, null)

        val postResult = mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $aliceToken")
        }.andReturn()
        val bookingId = objectMapper.readTree(postResult.response.contentAsString)["id"].asString()

        mockMvc.post("/api/bookings/$bookingId/cancel") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CancelBookingRequest("No longer needed"))
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("CANCELLED") }
            jsonPath("$.cancellation.reason") { value("No longer needed") }
        }

        val overlapping =
            CreateBookingRequest(roomId, bobId, LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 7), 5, null)
        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(overlapping)
            header("Authorization", "Bearer $bobToken")
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    fun `POST api-bookings-id-cancel returns 403 when user cancels another user booking`()
    {
        val roomId = createRoom()
        val postResult = mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), 5, null)
            )
            header("Authorization", "Bearer $aliceToken")
        }.andReturn()
        val bookingId = objectMapper.readTree(postResult.response.contentAsString)["id"].asString()

        mockMvc.post("/api/bookings/$bookingId/cancel") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CancelBookingRequest(null))
            header("Authorization", "Bearer $bobToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET api-bookings-id returns 403 when user accesses another user booking`()
    {
        val roomId = createRoom()
        val postResult = mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), 5, null)
            )
            header("Authorization", "Bearer $aliceToken")
        }.andReturn()
        val bookingId = objectMapper.readTree(postResult.response.contentAsString)["id"].asString()

        mockMvc.get("/api/bookings/$bookingId") {
            header("Authorization", "Bearer $bobToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET api-bookings-id returns 404 for unknown booking id`()
    {
        mockMvc.get("/api/bookings/${UUID.randomUUID()}") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST api-bookings returns 403 when user account is inactive`()
    {
        mockMvc.post("/api/users/$aliceId/validate") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect { status { isOk() } }
        mockMvc.post("/api/users/$aliceId/deactivate") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect { status { isOk() } }

        val roomId = createRoom()
        val request = CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), 5, null)

        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET api-bookings-my returns only the authenticated user bookings`()
    {
        val roomId = createRoom()
        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), 5, null)
            )
            header("Authorization", "Bearer $aliceToken")
        }.andExpect { status { isCreated() } }

        mockMvc.get("/api/bookings/my") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].user_id") { value(aliceId.toString()) }
        }

        mockMvc.get("/api/bookings/my") {
            header("Authorization", "Bearer $bobToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }
    }
}
