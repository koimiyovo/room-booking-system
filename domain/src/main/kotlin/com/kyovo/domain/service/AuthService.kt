package com.kyovo.domain.service

import com.kyovo.domain.exception.EmailAlreadyUsedException
import com.kyovo.domain.exception.InvalidCredentialsException
import com.kyovo.domain.model.user.NewUser
import com.kyovo.domain.model.user.User
import com.kyovo.domain.model.user.UserEmail
import com.kyovo.domain.model.user.UserRegistrationDate
import com.kyovo.domain.port.primary.AuthUseCase
import com.kyovo.domain.port.secondary.ClockPort
import com.kyovo.domain.port.secondary.PasswordHashPort
import com.kyovo.domain.port.secondary.UserRepository

class AuthService(
    private val userRepository: UserRepository,
    private val passwordHashPort: PasswordHashPort,
    private val clockPort: ClockPort
) : AuthUseCase
{
    override fun register(newUser: NewUser): User
    {
        if (userRepository.findByEmail(newUser.email) != null) throw EmailAlreadyUsedException(newUser.email)
        val hashed = newUser.copy(password = passwordHashPort.hash(newUser.password.value))
        val user = userRepository.save(hashed.toUser(UserRegistrationDate(clockPort.now())))
        userRepository.saveStatusHistory(user.id, user.statusInfo.status, user.statusInfo.since, null)
        return user
    }

    override fun login(email: UserEmail, rawPassword: String): User
    {
        val user = userRepository.findByEmail(email) ?: throw InvalidCredentialsException()
        if (!passwordHashPort.matches(rawPassword, user.password)) throw InvalidCredentialsException()
        return user
    }
}
