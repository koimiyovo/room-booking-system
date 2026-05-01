package com.kyovo.adapter.web.dto

import com.kyovo.domain.model.*

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
