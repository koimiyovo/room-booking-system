package com.kyovo.domain.model.booking

import com.kyovo.domain.model.room.RoomId
import com.kyovo.domain.model.user.UserId

data class Booking(
    val id: BookingId,
    val roomId: RoomId,
    val userId: UserId,
    val startDate: BookingStartDate,
    val endDate: BookingEndDate,
    val numberOfPeople: BookingNumberOfPeople,
    val specialRequests: BookingSpecialRequests?,
    val cancellation: Cancellation?
)
{
    val status: BookingStatus
        get()
        {
            return if (cancellation == null) BookingStatus.CONFIRMED else BookingStatus.CANCELLED
        }
}
