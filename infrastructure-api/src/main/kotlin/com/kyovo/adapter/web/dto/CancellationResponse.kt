package com.kyovo.adapter.web.dto

import java.util.UUID

data class CancellationResponse(
    val cancelledBy: UUID,
    val reason: String?
)
