package com.kyovo.domain.service

import com.kyovo.domain.exception.EmailAlreadyUsedException
import com.kyovo.domain.exception.UserNotFoundException
import com.kyovo.domain.model.*
import com.kyovo.domain.port.primary.UserUseCase
import com.kyovo.domain.port.secondary.PasswordHashPort
import com.kyovo.domain.port.secondary.UserRepository

class UserService(
    private val userRepository: UserRepository,
    private val passwordHashPort: PasswordHashPort
) : UserUseCase
{
    override fun findAll(): List<User>
    {
        return userRepository.findAll()
    }

    override fun findById(id: UserId): User?
    {
        return userRepository.findById(id)
    }

    override fun findByEmail(email: UserEmail): User?
    {
        return userRepository.findByEmail(email)
    }

    override fun save(newUser: NewUser): User
    {
        if (userRepository.findByEmail(newUser.email) != null) throw EmailAlreadyUsedException(newUser.email)
        val hashed = newUser.copy(password = passwordHashPort.hash(newUser.password.value))
        return userRepository.save(hashed.toUser())
    }

    override fun update(id: UserId, data: UpdateUser): User
    {
        val user = userRepository.findById(id) ?: throw UserNotFoundException(id)
        if (data.email != null && data.email != user.email) {
            if (userRepository.findByEmail(data.email) != null) throw EmailAlreadyUsedException(data.email)
        }
        val updated = user.copy(
            name = data.name ?: user.name,
            email = data.email ?: user.email,
            password = data.password?.let { passwordHashPort.hash(it.value) } ?: user.password
        )
        return userRepository.save(updated)
    }

    override fun delete(id: UserId)
    {
        if (userRepository.findById(id) == null) throw UserNotFoundException(id)
        userRepository.deleteById(id)
    }
}
