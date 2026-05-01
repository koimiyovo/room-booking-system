package com.kyovo.domain.service

import com.kyovo.domain.exception.EmailAlreadyUsedException
import com.kyovo.domain.exception.InvalidCredentialsException
import com.kyovo.domain.model.NewUser
import com.kyovo.domain.model.User
import com.kyovo.domain.model.UserEmail
import com.kyovo.domain.model.UserRegistrationDate
import com.kyovo.domain.port.primary.AuthUseCase
import com.kyovo.domain.port.secondary.PasswordHashPort
import com.kyovo.domain.port.secondary.UserRepository
import com.kyovo.domain.provider.TimeProvider

class AuthService(
    private val userRepository: UserRepository,
    private val passwordHashPort: PasswordHashPort,
    private val timeProvider: TimeProvider
) : AuthUseCase
{
    override fun register(newUser: NewUser): User
    {
        if (userRepository.findByEmail(newUser.email) != null) throw EmailAlreadyUsedException(newUser.email)
        val hashed = newUser.copy(password = passwordHashPort.hash(newUser.password.value))
        return userRepository.save(hashed.toUser(UserRegistrationDate(timeProvider.now())))
    }

    override fun login(email: UserEmail, rawPassword: String): User
    {
        val user = userRepository.findByEmail(email) ?: throw InvalidCredentialsException()
        if (!passwordHashPort.matches(rawPassword, user.password)) throw InvalidCredentialsException()
        return user
    }
}
