package com.kyovo.domain.model

data class Booking(
    val id: BookingId,
    val roomId: RoomId,
    val userId: UserId,
    val startDate: BookingStartDate,
    val endDate: BookingEndDate,
    val numberOfPeople: BookingNumberOfPeople,
    val specialRequests: BookingSpecialRequests?
)
