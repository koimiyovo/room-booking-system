package com.kyovo.adapter.web.dto

import com.kyovo.domain.model.User
import java.util.*

data class UserResponse(
    val id: UUID,
    val name: String,
    val email: String
)
{
    companion object
    {
        fun fromDomain(user: User): UserResponse
        {
            return UserResponse(
                id = user.id.value,
                name = user.name.value,
                email = user.email.value
            )
        }
    }
}
