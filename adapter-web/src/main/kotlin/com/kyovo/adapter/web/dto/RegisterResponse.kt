package com.kyovo.adapter.web.dto

import com.kyovo.domain.model.User
import java.time.LocalDateTime
import java.util.*

data class RegisterResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val registered_at: LocalDateTime,
)
{
    companion object
    {
        fun fromDomain(user: User): RegisterResponse
        {
            return RegisterResponse(
                id = user.id.value,
                name = user.name.value,
                email = user.email.value,
                registered_at = user.registeredAt.value
            )
        }
    }
}
