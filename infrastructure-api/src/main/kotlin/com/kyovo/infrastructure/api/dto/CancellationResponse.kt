package com.kyovo.infrastructure.api.dto

import java.util.*

data class CancellationResponse(
    val cancelledBy: UUID,
    val reason: String?
)
