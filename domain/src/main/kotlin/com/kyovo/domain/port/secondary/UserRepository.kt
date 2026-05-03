package com.kyovo.domain.port.secondary

import com.kyovo.domain.model.user.User
import com.kyovo.domain.model.user.UserEmail
import com.kyovo.domain.model.user.UserId
import com.kyovo.domain.model.user.UserStatus
import com.kyovo.domain.model.user.UserStatusInfoDate
import com.kyovo.domain.model.user.UserStatusReason

interface UserRepository
{
    fun findAll(): List<User>

    fun findById(id: UserId): User?

    fun findByEmail(email: UserEmail): User?

    fun save(user: User): User

    fun update(user: User): User

    fun saveStatusHistory(userId: UserId, status: UserStatus, since: UserStatusInfoDate, reason: UserStatusReason?)
}
