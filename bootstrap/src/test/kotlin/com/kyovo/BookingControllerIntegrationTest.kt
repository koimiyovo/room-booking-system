package com.kyovo

import com.fasterxml.jackson.databind.ObjectMapper
import com.kyovo.adapter.persistence.repository.BookingJpaRepository
import com.kyovo.adapter.persistence.repository.RoomJpaRepository
import com.kyovo.adapter.web.dto.CreateBookingRequest
import com.kyovo.adapter.web.dto.CreateRoomRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var bookingJpaRepository: BookingJpaRepository

    @Autowired
    private lateinit var roomJpaRepository: RoomJpaRepository

    @BeforeEach
    fun setUp() {
        bookingJpaRepository.deleteAll()
        roomJpaRepository.deleteAll()
    }

    private fun createRoom(name: String = "Conference Room", capacity: Int = 10): UUID {
        val result = mockMvc.post("/api/rooms") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateRoomRequest(name, capacity))
        }.andReturn()
        return UUID.fromString(objectMapper.readTree(result.response.contentAsString)["id"].asText())
    }

    @Test
    fun `POST then GET api-bookings returns the created booking`() {
        val roomId = createRoom()
        val userId = UUID.randomUUID()
        val request = CreateBookingRequest(roomId, userId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), 5, "Projector needed")

        val postResult = mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.roomId") { value(roomId.toString()) }
            jsonPath("$.userId") { value(userId.toString()) }
            jsonPath("$.numberOfPeople") { value(5) }
            jsonPath("$.specialRequests") { value("Projector needed") }
        }.andReturn()

        val createdId = objectMapper.readTree(postResult.response.contentAsString)["id"].asText()

        mockMvc.get("/api/bookings/$createdId")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(createdId) }
            }

        assertThat(bookingJpaRepository.count()).isEqualTo(1)
    }

    @Test
    fun `POST api-bookings returns 404 when room does not exist`() {
        val request = CreateBookingRequest(UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), 5, null)

        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST api-bookings returns 400 when capacity is exceeded`() {
        val roomId = createRoom(capacity = 5)
        val request = CreateBookingRequest(roomId, UUID.randomUUID(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), 10, null)

        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST api-bookings returns 409 when dates overlap for the same room`() {
        val roomId = createRoom()
        val userId = UUID.randomUUID()
        val request = CreateBookingRequest(roomId, userId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), 5, null)

        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect { status { isCreated() } }

        val overlapping = CreateBookingRequest(roomId, userId, LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 7), 5, null)
        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(overlapping)
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `GET api-bookings-id returns 404 for unknown booking id`() {
        mockMvc.get("/api/bookings/${UUID.randomUUID()}")
            .andExpect {
                status { isNotFound() }
            }
    }
}
