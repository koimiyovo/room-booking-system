package com.kyovo.domain.model.booking

import com.kyovo.domain.model.user.UserId

data class BookingStatusHistory(
    val bookingId: BookingId,
    val status: BookingStatus,
    val changedAt: BookingStatusHistoryDate,
    val changedBy: UserId?,
    val reason: BookingStatusReason?
)
