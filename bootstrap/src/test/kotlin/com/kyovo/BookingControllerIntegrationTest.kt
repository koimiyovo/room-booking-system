package com.kyovo

import com.kyovo.config.TestTimeProviderConfig
import com.kyovo.infrastructure.api.dto.*
import com.kyovo.infrastructure.persistence.entity.UserEntity
import com.kyovo.infrastructure.persistence.entity.UserStatusHistoryEntity
import com.kyovo.infrastructure.persistence.repository.BookingJpaRepository
import com.kyovo.infrastructure.persistence.repository.BookingStatusHistoryJpaRepository
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
    private lateinit var bookingStatusHistoryJpaRepository: BookingStatusHistoryJpaRepository

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
        bookingStatusHistoryJpaRepository.deleteAll()
        bookingJpaRepository.deleteAll()
        roomJpaRepository.deleteAll()
        userStatusHistoryJpaRepository.deleteAll()
        userJpaRepository.deleteAll()

        val savedAdmin = userJpaRepository.save(
            UserEntity(
                id = UUID.randomUUID(),
                name = "Admin",
                email = "admin@test.com",
                password = passwordEncoder.encode("admin123")!!,
                role = "ADMIN",
                registeredAt = OffsetDateTime.now(),
            )
        )
        userStatusHistoryJpaRepository.save(
            UserStatusHistoryEntity(id = UUID.randomUUID(), user = savedAdmin, status = "CREATED", since = OffsetDateTime.now(), until = null, reason = null)
        )
        adminToken = loginAndGetToken("admin@test.com", "admin123")

        val aliceResult = mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Alice", "alice@test.com", "alice123"))
        }.andReturn()
        aliceId = UUID.fromString(objectMapper.readTree(aliceResult.response.contentAsString)["id"].asString())
        aliceToken = loginAndGetToken("alice@test.com", "alice123")

        val bobResult = mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Bob", "bob@test.com", "bob123"))
        }.andReturn()
        bobId = UUID.fromString(objectMapper.readTree(bobResult.response.contentAsString)["id"].asString())
        bobToken = loginAndGetToken("bob@test.com", "bob123")
    }

    private fun loginAndGetToken(email: String, password: String): String
    {
        val result = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest(email, password))
        }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["token"].asString()
    }

    private fun createRoom(name: String = "Conference Room", capacity: Int = 10, requiresValidation: Boolean = false): UUID
    {
        val result = mockMvc.post("/api/v1/rooms") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateRoomRequest(name, capacity, requiresValidation))
            header("Authorization", "Bearer $adminToken")
        }.andReturn()
        return UUID.fromString(objectMapper.readTree(result.response.contentAsString)["id"].asString())
    }

    @Test
    fun `POST then GET api-v1-bookings returns the created booking`()
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

        val postResult = mockMvc.post("/api/v1/bookings") {
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

        mockMvc.get("/api/v1/bookings/$createdId") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(createdId) }
            jsonPath("$.status") { value("CONFIRMED") }
        }

        assertThat(bookingJpaRepository.count()).isEqualTo(1)
    }

    @Test
    fun `POST api-v1-bookings returns 404 when room does not exist`()
    {
        val request = CreateBookingRequest(
            UUID.randomUUID(),
            aliceId,
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 3),
            5,
            null
        )

        mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST api-v1-bookings returns 400 when capacity is exceeded`()
    {
        val roomId = createRoom(capacity = 5)
        val request =
            CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), 10, null)

        mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST api-v1-bookings returns 409 when dates overlap for the same room`()
    {
        val roomId = createRoom()
        val request = CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), 5, null)

        mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $aliceToken")
        }.andExpect { status { isCreated() } }

        val overlapping =
            CreateBookingRequest(roomId, bobId, LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 7), 5, null)
        mockMvc.post("/api/v1/bookings") {
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

        val postResult = mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $aliceToken")
        }.andReturn()
        val bookingId = objectMapper.readTree(postResult.response.contentAsString)["id"].asString()

        mockMvc.post("/api/v1/bookings/$bookingId/cancel") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CancelBookingRequest("No longer needed"))
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("CANCELLED") }
            jsonPath("$.status_info.reason") { value("No longer needed") }
            jsonPath("$.status_info.since") { exists() }
        }

        val overlapping =
            CreateBookingRequest(roomId, bobId, LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 7), 5, null)
        mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(overlapping)
            header("Authorization", "Bearer $bobToken")
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    fun `POST api-v1-bookings-id-cancel returns 403 when user cancels another user booking`()
    {
        val roomId = createRoom()
        val postResult = mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), 5, null)
            )
            header("Authorization", "Bearer $aliceToken")
        }.andReturn()
        val bookingId = objectMapper.readTree(postResult.response.contentAsString)["id"].asString()

        mockMvc.post("/api/v1/bookings/$bookingId/cancel") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CancelBookingRequest(null))
            header("Authorization", "Bearer $bobToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET api-v1-bookings-id returns 403 when user accesses another user booking`()
    {
        val roomId = createRoom()
        val postResult = mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), 5, null)
            )
            header("Authorization", "Bearer $aliceToken")
        }.andReturn()
        val bookingId = objectMapper.readTree(postResult.response.contentAsString)["id"].asString()

        mockMvc.get("/api/v1/bookings/$bookingId") {
            header("Authorization", "Bearer $bobToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET api-v1-bookings-id returns 404 for unknown booking id`()
    {
        mockMvc.get("/api/v1/bookings/${UUID.randomUUID()}") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST api-v1-bookings returns 403 when user account is inactive`()
    {
        mockMvc.post("/api/v1/users/$aliceId/validate") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect { status { isOk() } }
        mockMvc.post("/api/v1/users/$aliceId/deactivate") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect { status { isOk() } }

        val roomId = createRoom()
        val request = CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), 5, null)

        mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET api-v1-bookings-id-history returns CONFIRMED entry after creation`()
    {
        val roomId = createRoom()
        val postResult = mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), 5, null)
            )
            header("Authorization", "Bearer $aliceToken")
        }.andReturn()
        val bookingId = objectMapper.readTree(postResult.response.contentAsString)["id"].asString()

        mockMvc.get("/api/v1/bookings/$bookingId/history") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].status") { value("CONFIRMED") }
            jsonPath("$[0].changed_at") { exists() }
            jsonPath("$[0].changed_by") { value(aliceId.toString()) }
        }
    }

    @Test
    fun `GET api-v1-bookings-id-history returns CONFIRMED then CANCELLED after cancellation`()
    {
        val roomId = createRoom()
        val postResult = mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), 5, null)
            )
            header("Authorization", "Bearer $aliceToken")
        }.andReturn()
        val bookingId = objectMapper.readTree(postResult.response.contentAsString)["id"].asString()

        mockMvc.post("/api/v1/bookings/$bookingId/cancel") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CancelBookingRequest("No longer needed"))
            header("Authorization", "Bearer $aliceToken")
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/v1/bookings/$bookingId/history") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].status") { value("CONFIRMED") }
            jsonPath("$[1].status") { value("CANCELLED") }
            jsonPath("$[1].reason") { value("No longer needed") }
            jsonPath("$[1].changed_by") { value(aliceId.toString()) }
        }
    }

    @Test
    fun `GET api-v1-bookings-my returns only the authenticated user bookings`()
    {
        val roomId = createRoom()
        mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), 5, null)
            )
            header("Authorization", "Bearer $aliceToken")
        }.andExpect { status { isCreated() } }

        mockMvc.get("/api/v1/bookings/my") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].user_id") { value(aliceId.toString()) }
        }

        mockMvc.get("/api/v1/bookings/my") {
            header("Authorization", "Bearer $bobToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `POST api-v1-bookings returns PENDING when room requires validation`()
    {
        val roomId = createRoom(requiresValidation = true)
        val request = CreateBookingRequest(roomId, aliceId, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), 5, null)

        mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("PENDING") }
        }
    }

    @Test
    fun `POST api-v1-bookings-id-validate returns 200 and CONFIRMED booking`()
    {
        val roomId = createRoom(requiresValidation = true)
        val postResult = mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateBookingRequest(roomId, aliceId, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 3), 5, null)
            )
            header("Authorization", "Bearer $aliceToken")
        }.andReturn()
        val bookingId = objectMapper.readTree(postResult.response.contentAsString)["id"].asString()

        mockMvc.post("/api/v1/bookings/$bookingId/validate") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("CONFIRMED") }
            jsonPath("$.status_info.changed_by") { exists() }
        }
    }

    @Test
    fun `POST api-v1-bookings-id-validate cancels overlapping PENDING bookings`()
    {
        val roomId = createRoom(requiresValidation = true)
        val aliceResult = mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateBookingRequest(roomId, aliceId, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 5), 5, null)
            )
            header("Authorization", "Bearer $aliceToken")
        }.andReturn()
        val aliceBookingId = objectMapper.readTree(aliceResult.response.contentAsString)["id"].asString()

        val bobResult = mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateBookingRequest(roomId, bobId, LocalDate.of(2027, 2, 3), LocalDate.of(2027, 2, 7), 5, null)
            )
            header("Authorization", "Bearer $bobToken")
        }.andReturn()
        val bobBookingId = objectMapper.readTree(bobResult.response.contentAsString)["id"].asString()

        mockMvc.post("/api/v1/bookings/$aliceBookingId/validate") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/v1/bookings/$bobBookingId") {
            header("Authorization", "Bearer $bobToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("CANCELLED") }
        }
    }

    @Test
    fun `POST api-v1-bookings-id-validate returns 403 for non-admin`()
    {
        val roomId = createRoom(requiresValidation = true)
        val postResult = mockMvc.post("/api/v1/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateBookingRequest(roomId, aliceId, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 3), 5, null)
            )
            header("Authorization", "Bearer $aliceToken")
        }.andReturn()
        val bookingId = objectMapper.readTree(postResult.response.contentAsString)["id"].asString()

        mockMvc.post("/api/v1/bookings/$bookingId/validate") {
            header("Authorization", "Bearer $aliceToken")
        }.andExpect {
            status { isForbidden() }
        }
    }
}
