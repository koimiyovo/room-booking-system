package com.kyovo.infrastructure.api.dto

import com.kyovo.domain.model.user.*

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)
{
    fun toNewUser(): NewUser
    {
        return NewUser(
            name = UserName(name),
            email = UserEmail(email),
            password = UserPassword(password),
            role = UserRole.USER
        )
    }
}
