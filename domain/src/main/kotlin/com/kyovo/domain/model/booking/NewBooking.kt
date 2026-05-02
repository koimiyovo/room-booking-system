package com.kyovo.domain.model.booking

import com.kyovo.domain.model.room.RoomId
import com.kyovo.domain.model.user.UserId

data class NewBooking(
    val roomId: RoomId,
    val userId: UserId,
    val startDate: BookingStartDate,
    val endDate: BookingEndDate,
    val numberOfPeople: BookingNumberOfPeople,
    val specialRequests: BookingSpecialRequests?
)
{
    fun toBooking(): Booking
    {
        return Booking(BookingId.generate(), roomId, userId, startDate, endDate, numberOfPeople, specialRequests, null)
    }
}
