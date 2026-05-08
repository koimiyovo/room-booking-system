package com.kyovo.infrastructure.api.dto

import java.util.*

data class CreateRoomResponse(
    val id: UUID,
    val name: String,
    val capacity: Int,
    val requiresValidation: Boolean,
    val createdBy: UUID
)
