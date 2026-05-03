package com.kyovo.infrastructure.persistence.adapter

import com.kyovo.domain.model.user.*
import com.kyovo.domain.port.secondary.TransactionPort
import com.kyovo.domain.port.secondary.UserRepository

import com.kyovo.infrastructure.persistence.entity.UserEntity
import com.kyovo.infrastructure.persistence.entity.UserStatusHistoryEntity
import com.kyovo.infrastructure.persistence.repository.UserJpaRepository
import com.kyovo.infrastructure.persistence.repository.UserStatusHistoryJpaRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class UserPersistenceAdapter(
    private val jpaRepository: UserJpaRepository,
    private val statusHistoryJpaRepository: UserStatusHistoryJpaRepository,
    private val transactionPort: TransactionPort
) : UserRepository
{
    override fun findAll(): List<User>
    {
        return jpaRepository.findAllWithCurrentStatus().map { it.toUser() }
    }

    override fun findById(id: UserId): User?
    {
        return jpaRepository.findByIdWithCurrentStatus(id.value).firstOrNull()?.toUser()
    }

    override fun findByEmail(email: UserEmail): User?
    {
        return jpaRepository.findByEmailWithCurrentStatus(email.value).firstOrNull()?.toUser()
    }

    override fun save(user: User): User
    {
        val entity = jpaRepository.save(UserEntity.fromDomain(user))
        return entity.toDomain(user.statusInfo)
    }

    override fun update(user: User): User
    {
        val entity = jpaRepository.save(UserEntity.fromDomain(user))
        return entity.toDomain(user.statusInfo)
    }

    override fun saveStatusHistory(userId: UserId, status: UserStatus, since: UserStatusInfoDate, reason: UserStatusReason?)
    {
        transactionPort.executeInTransaction {
            statusHistoryJpaRepository.closeCurrentEntry(userId.value, since.value)
            val userRef = jpaRepository.getReferenceById(userId.value)
            statusHistoryJpaRepository.save(
                UserStatusHistoryEntity(
                    id = UUID.randomUUID(),
                    user = userRef,
                    status = status.label,
                    since = since.value,
                    until = null,
                    reason = reason?.value
                )
            )
        }
    }
}
