package com.kyovo.domain.port.primary

import com.kyovo.domain.model.booking.Booking
import com.kyovo.domain.model.booking.BookingCancellationReason
import com.kyovo.domain.model.booking.BookingId
import com.kyovo.domain.model.booking.NewBooking
import com.kyovo.domain.model.user.UserId

interface BookingUseCase
{
    fun findAll(): List<Booking>

    fun findById(id: BookingId): Booking?

    fun findByUserId(userId: UserId): List<Booking>

    fun create(newBooking: NewBooking): Booking

    fun cancel(bookingId: BookingId, cancelledBy: UserId, isAdmin: Boolean, reason: BookingCancellationReason?): Booking
}
