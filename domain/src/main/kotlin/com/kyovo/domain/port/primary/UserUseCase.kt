package com.kyovo.domain.port.primary

import com.kyovo.domain.model.*

interface UserUseCase
{
    fun findAll(): List<User>

    fun findById(id: UserId): User?

    fun save(newUser: NewUser): User
}
