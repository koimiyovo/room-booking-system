package com.kyovo.domain.model.booking

import com.kyovo.domain.model.user.UserId

data class BookingStatusInfo(
    val status: BookingStatus,
    val since: BookingStatusInfoDate,
    val changedBy: UserId?,
    val reason: BookingStatusReason?
)
