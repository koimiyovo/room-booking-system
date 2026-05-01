package com.kyovo.domain.port.primary

import com.kyovo.domain.model.*

interface BookingUseCase
{
    fun findAll(): List<Booking>

    fun findById(id: BookingId): Booking?

    fun findByUserId(userId: UserId): List<Booking>

    fun create(newBooking: NewBooking): Booking

    fun cancel(bookingId: BookingId, cancelledBy: UserId, isAdmin: Boolean, reason: BookingCancellationReason?): Booking
}
