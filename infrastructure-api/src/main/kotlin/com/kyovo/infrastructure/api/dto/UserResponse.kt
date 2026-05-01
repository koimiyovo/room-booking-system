package com.kyovo.infrastructure.api.dto

import com.kyovo.domain.model.user.User
import java.util.*

data class UserResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val role: String
)
{
    companion object
    {
        fun fromDomain(user: User): UserResponse
        {
            return UserResponse(
                id = user.id.value,
                name = user.name.value,
                email = user.email.value,
                role = user.role.label
            )
        }
    }
}
