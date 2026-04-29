package com.kyovo.domain.service

import com.kyovo.domain.model.NewUser
import com.kyovo.domain.model.User
import com.kyovo.domain.model.UserId
import com.kyovo.domain.port.primary.UserUseCase
import com.kyovo.domain.port.secondary.UserRepository

class UserService(private val userRepository: UserRepository) : UserUseCase
{
    override fun findAll(): List<User>
    {
        return userRepository.findAll()
    }

    override fun findById(id: UserId): User?
    {
        return userRepository.findById(id)
    }

    override fun save(newUser: NewUser): User
    {
        return userRepository.save(newUser.toUser())
    }
}
