package com.kyovo.adapter.web.dto

import java.util.UUID

data class CreateRoomResponse(
    val id: UUID,
    val name: String,
    val capacity: Int
)
