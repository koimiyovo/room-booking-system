package com.kyovo.adapter.persistence.adapter

import com.kyovo.adapter.persistence.entity.UserEntity
import com.kyovo.adapter.persistence.repository.UserJpaRepository
import com.kyovo.domain.model.*
import com.kyovo.domain.port.secondary.UserRepository
import org.springframework.stereotype.Component

@Component
class UserPersistenceAdapter(private val jpaRepository: UserJpaRepository) : UserRepository
{
    override fun findAll(): List<User>
    {
        return jpaRepository.findAll().map { it.toDomain() }
    }

    override fun findById(id: UserId): User?
    {
        return jpaRepository.findById(id.value).orElse(null)?.toDomain()
    }

    override fun findByEmail(email: UserEmail): User?
    {
        return jpaRepository.findByEmail(email.value)?.toDomain()
    }

    override fun save(user: User): User
    {
        val entity = UserEntity.fromDomain(user)
        return jpaRepository.save(entity).toDomain()
    }

    override fun deleteById(id: UserId)
    {
        jpaRepository.deleteById(id.value)
    }
}
