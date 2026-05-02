package com.kyovo.domain.service

import com.kyovo.domain.exception.AccountNotOwnedByUserException
import com.kyovo.domain.exception.EmailAlreadyUsedException
import com.kyovo.domain.exception.InvalidStatusTransitionException
import com.kyovo.domain.exception.UserNotFoundException
import com.kyovo.domain.model.user.*
import com.kyovo.domain.port.primary.UserUseCase
import com.kyovo.domain.port.secondary.ClockPort
import com.kyovo.domain.port.secondary.PasswordHashPort
import com.kyovo.domain.port.secondary.TransactionPort
import com.kyovo.domain.port.secondary.UserRepository

class UserService(
    private val userRepository: UserRepository,
    private val passwordHashPort: PasswordHashPort,
    private val transactionPort: TransactionPort,
    private val clockPort: ClockPort
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

    override fun update(id: UserId, data: UpdateUser): User
    {
        val user = userRepository.findById(id) ?: throw UserNotFoundException(id)
        if (data.email != null && data.email != user.email)
        {
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

    override fun validate(id: UserId, isAdmin: Boolean, validateBy: UserId): User
    {
        return transactionPort.executeInTransaction {
            val user = userRepository.findById(id) ?: throw UserNotFoundException(id)
            if (!isAdmin && user.id != validateBy)
                throw AccountNotOwnedByUserException()
            if (user.statusInfo.status != UserStatus.CREATED)
                throw InvalidStatusTransitionException(user.statusInfo.status, UserStatus.ACTIVE)
            persistTransition(user, UserStatus.ACTIVE)
        }
    }

    override fun deactivate(id: UserId): User
    {
        val user = userRepository.findById(id) ?: throw UserNotFoundException(id)
        return persistTransition(user, UserStatus.INACTIVE)
    }

    override fun reactivate(id: UserId): User
    {
        val user = userRepository.findById(id) ?: throw UserNotFoundException(id)
        if (user.statusInfo.status != UserStatus.INACTIVE)
            throw InvalidStatusTransitionException(user.statusInfo.status, UserStatus.ACTIVE)
        return persistTransition(user, UserStatus.ACTIVE)
    }

    private fun persistTransition(user: User, target: UserStatus): User
    {
        val now = UserStatusInfoDate(clockPort.now())
        val updated =
            user.transitionTo(target, now) ?: throw InvalidStatusTransitionException(user.statusInfo.status, target)

        return transactionPort.executeInTransaction {
            val saved = userRepository.update(updated)
            userRepository.saveStatusHistory(saved.id, saved.statusInfo.status, saved.statusInfo.since)
            saved
        }
    }
}
