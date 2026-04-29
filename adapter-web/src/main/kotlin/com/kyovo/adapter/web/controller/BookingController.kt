package com.kyovo.adapter.web.controller

import com.kyovo.adapter.web.dto.BookingResponse
import com.kyovo.adapter.web.dto.CancelBookingRequest
import com.kyovo.adapter.web.dto.CreateBookingRequest
import com.kyovo.adapter.web.dto.CreateBookingResponse
import com.kyovo.domain.model.BookingCancellationReason
import com.kyovo.domain.model.BookingId
import com.kyovo.domain.model.UserId
import com.kyovo.domain.model.UserRole.ADMIN
import com.kyovo.domain.port.primary.BookingUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
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

    @GetMapping("/my")
    @Operation(summary = "List bookings of the authenticated user")
    @ApiResponse(responseCode = "200", description = "Booking list returned successfully")
    fun findMine(authentication: Authentication): List<BookingResponse>
    {
        val userId = UserId(UUID.fromString(authentication.name))
        return bookingUseCase.findByUserId(userId).map { BookingResponse.fromDomain(it) }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a booking by its identifier")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Booking found"),
        ApiResponse(responseCode = "403", description = "Access denied"),
        ApiResponse(responseCode = "404", description = "Booking not found")
    )
    fun findById(
        @Parameter(description = "UUID identifier of the booking")
        @PathVariable id: UUID,
        authentication: Authentication
    ): ResponseEntity<BookingResponse>
    {
        val booking = bookingUseCase.findById(BookingId(id)) ?: return ResponseEntity.notFound().build()
        val isAdmin = authentication.authorities.any { it.authority == "ROLE_${ADMIN.label}" }
        val requestingUserId = UserId(UUID.fromString(authentication.name))
        if (!isAdmin && booking.userId != requestingUserId) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        return ResponseEntity.ok(BookingResponse.fromDomain(booking))
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

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
        ApiResponse(responseCode = "403", description = "Access denied"),
        ApiResponse(responseCode = "404", description = "Booking not found"),
        ApiResponse(responseCode = "409", description = "Booking is already cancelled")
    )
    fun cancel(
        @PathVariable id: UUID,
        @RequestBody(required = false) request: CancelBookingRequest?,
        authentication: Authentication
    ): ResponseEntity<BookingResponse>
    {
        val bookingId = BookingId(id)
        val isAdmin = authentication.authorities.any { it.authority == "ROLE_${ADMIN.label}" }
        val cancelledByUserId = UserId(UUID.fromString(authentication.name))
        val reason = request?.reason?.let { BookingCancellationReason(it) }
        val booking = bookingUseCase.cancel(bookingId, cancelledByUserId, isAdmin, reason)
        return ResponseEntity.ok(BookingResponse.fromDomain(booking))
    }
}
