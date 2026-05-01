package com.kyovo.infrastructure.api.dto

import com.kyovo.domain.model.user.UpdateUser
import com.kyovo.domain.model.user.UserEmail
import com.kyovo.domain.model.user.UserName
import com.kyovo.domain.model.user.UserPassword

data class UpdateUserRequest(
    val name: String?,
    val email: String?,
    val password: String?
)
{
    fun toUpdateUser(): UpdateUser
    {
        return UpdateUser(
            name = name?.let { UserName(it) },
            email = email?.let { UserEmail(it) },
            password = password?.let { UserPassword(it) }
        )
    }
}
