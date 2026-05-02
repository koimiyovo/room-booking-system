package com.kyovo.infrastructure.api.dto

import com.kyovo.domain.model.user.User
import java.time.OffsetDateTime
import java.util.*

data class RegisterResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val registeredAt: OffsetDateTime,
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
                registeredAt = user.registeredAt.value
            )
        }
    }
}
