package com.kyovo.domain.port.secondary

import com.kyovo.domain.model.Booking
import com.kyovo.domain.model.BookingEndDate
import com.kyovo.domain.model.BookingId
import com.kyovo.domain.model.BookingStartDate
import com.kyovo.domain.model.RoomId
import com.kyovo.domain.model.UserId

interface BookingRepository
{
    fun findAll(): List<Booking>

    fun findById(id: BookingId): Booking?

    fun findByUserId(userId: UserId): List<Booking>

    fun save(booking: Booking): Booking

    fun update(booking: Booking): Booking

    fun existsOverlappingBooking(roomId: RoomId, startDate: BookingStartDate, endDate: BookingEndDate): Boolean
}
