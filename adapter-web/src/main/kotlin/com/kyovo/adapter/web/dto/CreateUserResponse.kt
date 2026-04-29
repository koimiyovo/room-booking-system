package com.kyovo.adapter.web.dto

import java.util.UUID

data class CreateUserResponse(
    val id: UUID,
    val name: String,
    val email: String
)
