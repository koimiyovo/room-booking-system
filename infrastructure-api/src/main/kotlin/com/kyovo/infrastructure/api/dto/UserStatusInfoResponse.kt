package com.kyovo.infrastructure.api.dto

import java.time.OffsetDateTime

data class UserStatusInfoResponse(
    val status: UserStatusResponse,
    val since: OffsetDateTime,
    val reason: String?
)
