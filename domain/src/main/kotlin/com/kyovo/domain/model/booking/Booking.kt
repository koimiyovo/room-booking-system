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
    val statusInfo: BookingStatusInfo
)
{
    val status: BookingStatus get() = statusInfo.status

    fun transitionTo(
        target: BookingStatus,
        now: BookingStatusInfoDate,
        changedBy: UserId?,
        reason: BookingStatusReason?
    ): Booking?
    {
        if (!statusInfo.status.canTransitionTo(target)) return null
        return copy(statusInfo = BookingStatusInfo(target, now, changedBy, reason))
    }
}
