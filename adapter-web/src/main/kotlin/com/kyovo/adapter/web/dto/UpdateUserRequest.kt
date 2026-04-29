package com.kyovo.adapter.web.dto

import com.kyovo.domain.model.UpdateUser
import com.kyovo.domain.model.UserEmail
import com.kyovo.domain.model.UserName
import com.kyovo.domain.model.UserPassword

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
