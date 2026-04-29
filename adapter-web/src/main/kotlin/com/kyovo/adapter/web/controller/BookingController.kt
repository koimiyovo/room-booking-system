package com.kyovo.adapter.web.controller

import com.kyovo.adapter.web.dto.BookingResponse
import com.kyovo.adapter.web.dto.CreateBookingRequest
import com.kyovo.adapter.web.dto.CreateBookingResponse
import com.kyovo.domain.model.BookingId
import com.kyovo.domain.port.primary.BookingUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Booking management")
class BookingController(private val bookingUseCase: BookingUseCase)
{
    @GetMapping
    @Operation(summary = "List all bookings")
    @ApiResponse(responseCode = "200", description = "Booking list returned successfully")
    fun findAll(): List<BookingResponse>
    {
        return bookingUseCase.findAll().map { BookingResponse.fromDomain(it) }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a booking by its identifier")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Booking found"),
        ApiResponse(responseCode = "404", description = "Booking not found")
    )
    fun findById(
        @Parameter(description = "UUID identifier of the booking")
        @PathVariable id: UUID
    ): ResponseEntity<BookingResponse>
    {
        return bookingUseCase.findById(BookingId(id))
            ?.let { ResponseEntity.ok(BookingResponse.fromDomain(it)) }
            ?: ResponseEntity.notFound().build()
    }

    @PostMapping
    @Operation(summary = "Create a new booking")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Booking created successfully"),
        ApiResponse(responseCode = "400", description = "Room capacity exceeded"),
        ApiResponse(responseCode = "404", description = "Room not found"),
        ApiResponse(responseCode = "409", description = "Booking conflict for this room and period")
    )
    fun create(@RequestBody request: CreateBookingRequest): ResponseEntity<CreateBookingResponse>
    {
        val booking = bookingUseCase.create(request.toNewBooking())
        val response = CreateBookingResponse(
            id = booking.id.value,
            roomId = booking.roomId.value,
            userId = booking.userId.value,
            startDate = booking.startDate.value,
            endDate = booking.endDate.value,
            numberOfPeople = booking.numberOfPeople.value,
            specialRequests = booking.specialRequests?.value
        )
        return ResponseEntity(response, HttpStatus.CREATED)
    }
}
