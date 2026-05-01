package com.kyovo.infrastructure.api.dto

import com.kyovo.domain.model.user.UserStatus

enum class UserStatusResponse
{
    CREATED;

    companion object
    {
        fun from(status: UserStatus): UserStatusResponse
        {
            return when (status)
            {
                UserStatus.CREATED -> CREATED
            }
        }
    }
}