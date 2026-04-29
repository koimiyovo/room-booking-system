package com.kyovo.domain.exception

import com.kyovo.domain.model.BookingEndDate
import com.kyovo.domain.model.BookingStartDate
import com.kyovo.domain.model.RoomId

class BookingConflictException(
    val roomId: RoomId,
    val startDate: BookingStartDate,
    val endDate: BookingEndDate
) : RuntimeException("Room ${roomId.value} is already booked from ${startDate.value} to ${endDate.value}")
