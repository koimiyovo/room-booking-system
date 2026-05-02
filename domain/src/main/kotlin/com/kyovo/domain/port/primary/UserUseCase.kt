package com.kyovo.domain.port.primary

import com.kyovo.domain.model.user.UpdateUser
import com.kyovo.domain.model.user.User
import com.kyovo.domain.model.user.UserId
import com.kyovo.domain.model.user.UserStatusReason

interface UserUseCase
{
    fun findAll(): List<User>

    fun findById(id: UserId): User?

    fun update(id: UserId, data: UpdateUser): User

    fun delete(id: UserId, reason: UserStatusReason?)

    fun validate(id: UserId, isAdmin: Boolean, validateBy: UserId, reason: UserStatusReason?): User

    fun deactivate(id: UserId, reason: UserStatusReason?): User

    fun reactivate(id: UserId, reason: UserStatusReason?): User
}
