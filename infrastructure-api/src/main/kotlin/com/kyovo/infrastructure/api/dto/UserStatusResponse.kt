package com.kyovo.infrastructure.api.dto

import com.kyovo.domain.model.user.UserStatus

enum class UserStatusResponse
{
    CREATED,
    ACTIVE,
    INACTIVE,
    DELETED;

    companion object
    {
        fun from(status: UserStatus): UserStatusResponse
        {
            return when (status)
            {
                UserStatus.CREATED -> CREATED
                UserStatus.ACTIVE -> ACTIVE
                UserStatus.INACTIVE -> INACTIVE
                UserStatus.DELETED -> DELETED
            }
        }
    }
}