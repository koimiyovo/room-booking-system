package com.kyovo.domain.port.secondary

import com.kyovo.domain.model.booking.*
import com.kyovo.domain.model.room.RoomId
import com.kyovo.domain.model.user.UserId

interface BookingRepository
{
    fun findAll(): List<Booking>

    fun findById(id: BookingId): Booking?

    fun findByUserId(userId: UserId): List<Booking>

    fun save(booking: Booking): Booking

    fun update(booking: Booking): Booking

    fun existsOverlappingBooking(roomId: RoomId, startDate: BookingStartDate, endDate: BookingEndDate): Boolean

    fun saveStatusHistory(
        bookingId: BookingId,
        status: BookingStatus,
        changedAt: BookingStatusHistoryDate,
        changedBy: UserId?,
        reason: BookingStatusReason?
    )

    fun findStatusHistory(bookingId: BookingId): List<BookingStatusHistory>
}
