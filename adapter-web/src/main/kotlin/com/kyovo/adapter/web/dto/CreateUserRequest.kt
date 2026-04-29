package com.kyovo.adapter.web.dto

import com.kyovo.domain.model.*

data class CreateUserRequest(
    val name: String,
    val email: String
)
{
    fun toNewUser(): NewUser
    {
        return NewUser(
            name = UserName(name),
            email = UserEmail(email)
        )
    }
}
