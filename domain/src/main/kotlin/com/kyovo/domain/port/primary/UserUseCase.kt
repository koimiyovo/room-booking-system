package com.kyovo.domain.port.primary

import com.kyovo.domain.model.user.UpdateUser
import com.kyovo.domain.model.user.User
import com.kyovo.domain.model.user.UserId

interface UserUseCase
{
    fun findAll(): List<User>

    fun findById(id: UserId): User?

    fun update(id: UserId, data: UpdateUser): User

    fun delete(id: UserId)

    fun validate(id: UserId, isAdmin: Boolean, validateBy: UserId): User

    fun deactivate(id: UserId): User

    fun reactivate(id: UserId): User
}
