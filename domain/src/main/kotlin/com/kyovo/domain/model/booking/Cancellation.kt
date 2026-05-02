package com.kyovo.domain.model.booking

import com.kyovo.domain.model.user.UserId

data class Cancellation(
    val cancelledBy: UserId,
    val reason: BookingCancellationReason?
)
