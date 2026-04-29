package com.kyovo.domain.model

data class Cancellation(
    val cancelledBy: UserId,
    val reason: BookingCancellationReason?
)
