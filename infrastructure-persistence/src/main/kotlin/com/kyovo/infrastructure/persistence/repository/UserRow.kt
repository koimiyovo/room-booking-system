package com.kyovo.infrastructure.persistence.repository

import com.kyovo.domain.model.user.User
import com.kyovo.infrastructure.persistence.entity.UserEntity
import com.kyovo.infrastructure.persistence.entity.UserStatusHistoryEntity

data class UserRow(val user: UserEntity, val status: UserStatusHistoryEntity)
{
    fun toUser(): User
    {
        return user.toDomain(status.toStatusInfo())
    }
}
