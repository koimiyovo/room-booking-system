package com.kyovo.domain.port.secondary

import com.kyovo.domain.model.Booking
import com.kyovo.domain.model.BookingEndDate
import com.kyovo.domain.model.BookingId
import com.kyovo.domain.model.BookingStartDate
import com.kyovo.domain.model.RoomId

interface BookingRepository
{
    fun findAll(): List<Booking>

    fun findById(id: BookingId): Booking?

    fun save(booking: Booking): Booking

    fun existsOverlappingBooking(roomId: RoomId, startDate: BookingStartDate, endDate: BookingEndDate): Boolean
}
