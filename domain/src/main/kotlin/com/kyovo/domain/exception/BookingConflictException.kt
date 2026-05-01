package com.kyovo.domain.exception

import com.kyovo.domain.model.booking.BookingEndDate
import com.kyovo.domain.model.booking.BookingStartDate
import com.kyovo.domain.model.room.RoomId

class BookingConflictException(
    val roomId: RoomId,
    val startDate: BookingStartDate,
    val endDate: BookingEndDate
) : RuntimeException("Room ${roomId.value} is already booked from ${startDate.value} to ${endDate.value}")
