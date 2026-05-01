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
    override fun findAll(): List<User> = jpaRepository.findAll().map { it.toDomain() }

    override fun findById(id: UserId): User? = jpaRepository.findById(id.value).orElse(null)?.toDomain()

    override fun findByEmail(email: UserEmail): User? = jpaRepository.findByEmail(email.value)?.toDomain()

    override fun save(user: User): User = jpaRepository.save(UserEntity.fromDomain(user)).toDomain()

    override fun deleteById(id: UserId)
    {
        jpaRepository.deleteById(id.value)
    }

    override fun update(user: User): User = jpaRepository.save(UserEntity.fromDomain(user)).toDomain()

    override fun saveStatusHistory(userId: UserId, status: UserStatus, since: UserStatusInfoDate)
    {
        transactionPort.executeInTransaction {
            statusHistoryJpaRepository.closeCurrentEntry(userId.value, since.value)
            statusHistoryJpaRepository.save(
                UserStatusHistoryEntity(
                    id = UUID.randomUUID(),
                    userId = userId.value,
                    status = status.label,
                    since = since.value,
                    until = null
                )
            )
        }
    }
}
