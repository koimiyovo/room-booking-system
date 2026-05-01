package com.kyovo.infrastructure.api.dto

import com.kyovo.domain.model.user.UserRole

enum class UserRoleResponse
{
    ADMIN,
    USER;

    companion object
    {
        fun from(role: UserRole): UserRoleResponse
        {
            return when (role)
            {
                UserRole.ADMIN -> ADMIN
                UserRole.USER -> USER
            }
        }
    }
}