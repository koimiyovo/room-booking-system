package com.kyovo.adapter.web.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.kyovo.adapter.web.dto.CreateBookingRequest
import com.kyovo.domain.exception.BookingConflictException
import com.kyovo.domain.exception.RoomCapacityExceededException
import com.kyovo.domain.exception.RoomNotFoundException
import com.kyovo.domain.model.*
import com.kyovo.domain.port.primary.BookingUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(BookingController::class)
class BookingControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var bookingUseCase: BookingUseCase

    private val bookingId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    private val roomId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001")
    private val userId = UUID.fromString("770e8400-e29b-41d4-a716-446655440002")
    private val startDate = LocalDate.of(2026, 6, 1)
    private val endDate = LocalDate.of(2026, 6, 3)

    private val booking = Booking(
        id = BookingId(bookingId),
        roomId = RoomId(roomId),
        userId = UserId(userId),
        startDate = BookingStartDate(startDate),
        endDate = BookingEndDate(endDate),
        numberOfPeople = BookingNumberOfPeople(5),
        specialRequests = null
    )

    private val request = CreateBookingRequest(roomId, userId, startDate, endDate, 5, null)

    @Test
    fun `GET api-bookings returns 200 with the list of bookings`() {
        whenever(bookingUseCase.findAll()).thenReturn(listOf(booking))

        mockMvc.get("/api/bookings")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(bookingId.toString()) }
                jsonPath("$[0].roomId") { value(roomId.toString()) }
                jsonPath("$[0].userId") { value(userId.toString()) }
            }
    }

    @Test
    fun `GET api-bookings returns 200 with an empty list`() {
        whenever(bookingUseCase.findAll()).thenReturn(emptyList())

        mockMvc.get("/api/bookings")
            .andExpect {
                status { isOk() }
                content { json("[]") }
            }
    }

    @Test
    fun `GET api-bookings-id returns 200 with the booking when it exists`() {
        whenever(bookingUseCase.findById(BookingId(bookingId))).thenReturn(booking)

        mockMvc.get("/api/bookings/$bookingId")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(bookingId.toString()) }
            }
    }

    @Test
    fun `GET api-bookings-id returns 404 when the booking does not exist`() {
        whenever(bookingUseCase.findById(BookingId(bookingId))).thenReturn(null)

        mockMvc.get("/api/bookings/$bookingId")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `POST api-bookings returns 201 with the created booking`() {
        whenever(bookingUseCase.create(any())).thenReturn(booking)

        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { value(bookingId.toString()) }
            jsonPath("$.roomId") { value(roomId.toString()) }
            jsonPath("$.userId") { value(userId.toString()) }
            jsonPath("$.numberOfPeople") { value(5) }
        }
    }

    @Test
    fun `POST api-bookings returns 404 when room does not exist`() {
        whenever(bookingUseCase.create(any())).thenThrow(RoomNotFoundException(RoomId(roomId)))

        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST api-bookings returns 400 when capacity is exceeded`() {
        whenever(bookingUseCase.create(any())).thenThrow(
            RoomCapacityExceededException(BookingNumberOfPeople(15), RoomCapacity(10))
        )

        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST api-bookings returns 409 when booking conflicts`() {
        whenever(bookingUseCase.create(any())).thenThrow(
            BookingConflictException(RoomId(roomId), BookingStartDate(startDate), BookingEndDate(endDate))
        )

        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
        }
    }
}
