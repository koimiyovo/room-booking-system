package com.kyovo.infrastructure.api.controller

import com.kyovo.domain.exception.BookingAlreadyCancelledException
import com.kyovo.domain.exception.BookingConflictException
import com.kyovo.domain.exception.RoomCapacityExceededException
import com.kyovo.domain.exception.RoomNotFoundException
import com.kyovo.domain.model.booking.*
import com.kyovo.domain.model.room.RoomCapacity
import com.kyovo.domain.model.room.RoomId
import com.kyovo.domain.model.user.UserId
import com.kyovo.domain.port.primary.BookingUseCase
import com.kyovo.infrastructure.api.dto.CancelBookingRequest
import com.kyovo.infrastructure.api.dto.CreateBookingRequest
import com.kyovo.infrastructure.api.security.JwtService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.util.*

@WebMvcTest(BookingController::class)
class BookingControllerWebMvcTest
{
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var bookingUseCase: BookingUseCase

    @MockitoBean
    private lateinit var jwtService: JwtService

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
        specialRequests = null,
        cancellation = null
    )

    private val request = CreateBookingRequest(roomId, userId, startDate, endDate, 5, null)

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `GET api-bookings returns 200 with the list of bookings`()
    {
        whenever(bookingUseCase.findAll()).thenReturn(listOf(booking))

        mockMvc.get("/api/bookings")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(bookingId.toString()) }
                jsonPath("$[0].status") { value("CONFIRMED") }
            }
    }

    @Test
    @WithMockUser(username = "770e8400-e29b-41d4-a716-446655440002", roles = ["USER"])
    fun `GET api-bookings-my returns 200 with the authenticated user bookings`()
    {
        whenever(bookingUseCase.findByUserId(UserId(userId))).thenReturn(listOf(booking))

        mockMvc.get("/api/bookings/my")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(bookingId.toString()) }
            }
    }

    @Test
    @WithMockUser(username = "770e8400-e29b-41d4-a716-446655440002", roles = ["USER"])
    fun `GET api-bookings-id returns 200 when user accesses own booking`()
    {
        whenever(bookingUseCase.findById(BookingId(bookingId))).thenReturn(booking)

        mockMvc.get("/api/bookings/$bookingId")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(bookingId.toString()) }
            }
    }

    @Test
    @WithMockUser(username = "99999999-9999-9999-9999-999999999999", roles = ["USER"])
    fun `GET api-bookings-id returns 403 when user accesses another user booking`()
    {
        whenever(bookingUseCase.findById(BookingId(bookingId))).thenReturn(booking)

        mockMvc.get("/api/bookings/$bookingId")
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `GET api-bookings-id returns 404 when the booking does not exist`()
    {
        whenever(bookingUseCase.findById(BookingId(bookingId))).thenReturn(null)

        mockMvc.get("/api/bookings/$bookingId")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `POST api-bookings returns 201 with the created booking`()
    {
        whenever(bookingUseCase.create(any())).thenReturn(booking)

        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(csrf())
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { value(bookingId.toString()) }
            jsonPath("$.number_of_people") { value(5) }
        }
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `POST api-bookings returns 404 when room does not exist`()
    {
        whenever(bookingUseCase.create(any())).thenThrow(RoomNotFoundException(RoomId(roomId)))

        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(csrf())
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `POST api-bookings returns 400 when capacity is exceeded`()
    {
        whenever(bookingUseCase.create(any())).thenThrow(
            RoomCapacityExceededException(BookingNumberOfPeople(15), RoomCapacity(10))
        )

        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(csrf())
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `POST api-bookings returns 409 when booking conflicts`()
    {
        whenever(bookingUseCase.create(any())).thenThrow(
            BookingConflictException(RoomId(roomId), BookingStartDate(startDate), BookingEndDate(endDate))
        )

        mockMvc.post("/api/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(csrf())
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    @WithMockUser(username = "770e8400-e29b-41d4-a716-446655440002", roles = ["USER"])
    fun `POST api-bookings-id-cancel returns 200 with cancelled booking`()
    {
        val cancelledBooking =
            booking.copy(cancellation = Cancellation(UserId(userId), BookingCancellationReason("Change of plans")))
        whenever(bookingUseCase.cancel(any(), any(), any(), anyOrNull())).thenReturn(cancelledBooking)

        mockMvc.post("/api/bookings/$bookingId/cancel") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CancelBookingRequest("Change of plans"))
            with(csrf())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("CANCELLED") }
            jsonPath("$.cancellation.reason") { value("Change of plans") }
            jsonPath("$.cancellation.cancelled_by") { value(userId.toString()) }
        }
    }

    @Test
    @WithMockUser(username = "770e8400-e29b-41d4-a716-446655440002", roles = ["USER"])
    fun `POST api-bookings-id-cancel returns 409 when booking is already cancelled`()
    {
        whenever(bookingUseCase.cancel(any(), any(), any(), anyOrNull()))
            .thenThrow(BookingAlreadyCancelledException(BookingId(bookingId)))

        mockMvc.post("/api/bookings/$bookingId/cancel") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CancelBookingRequest(null))
            with(csrf())
        }.andExpect {
            status { isConflict() }
        }
    }
}
