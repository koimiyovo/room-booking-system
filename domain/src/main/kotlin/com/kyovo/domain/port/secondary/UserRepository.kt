package com.kyovo.domain.port.secondary

import com.kyovo.domain.model.user.User
import com.kyovo.domain.model.user.UserEmail
import com.kyovo.domain.model.user.UserId

interface UserRepository
{
    fun findAll(): List<User>

    fun findById(id: UserId): User?

    fun findByEmail(email: UserEmail): User?

    fun save(user: User): User

    fun deleteById(id: UserId)
}
