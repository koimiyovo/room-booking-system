package com.kyovo.domain.port.primary

import com.kyovo.domain.model.booking.*
import com.kyovo.domain.model.user.UserId

interface BookingUseCase
{
    fun findAll(): List<Booking>

    fun findById(id: BookingId): Booking?

    fun findByUserId(userId: UserId): List<Booking>

    fun create(newBooking: NewBooking): Booking

    fun cancel(bookingId: BookingId, cancelledBy: UserId, isAdmin: Boolean, reason: BookingStatusReason?): Booking

    fun findStatusHistory(bookingId: BookingId): List<BookingStatusHistory>

    fun validate(bookingId: BookingId, adminId: UserId): Booking
}
