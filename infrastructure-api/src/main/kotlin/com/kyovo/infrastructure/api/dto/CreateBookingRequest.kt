package com.kyovo.infrastructure.api.dto

import com.kyovo.domain.model.booking.*
import com.kyovo.domain.model.room.RoomId
import com.kyovo.domain.model.user.UserId
import java.time.LocalDate
import java.util.*

data class CreateBookingRequest(
    val roomId: UUID,
    val userId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val numberOfPeople: Int,
    val specialRequests: String?
)
{
    fun toNewBooking(): NewBooking
    {
        return NewBooking(
            roomId = RoomId(roomId),
            userId = UserId(userId),
            startDate = BookingStartDate(startDate),
            endDate = BookingEndDate(endDate),
            numberOfPeople = BookingNumberOfPeople(numberOfPeople),
            specialRequests = specialRequests?.let { BookingSpecialRequests(it) }
        )
    }
}
