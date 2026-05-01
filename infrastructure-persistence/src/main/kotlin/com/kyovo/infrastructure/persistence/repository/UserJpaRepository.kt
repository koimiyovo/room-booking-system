package com.kyovo.infrastructure.persistence.repository

import com.kyovo.infrastructure.persistence.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserJpaRepository : JpaRepository<UserEntity, UUID>
{
    fun findByEmail(email: String): UserEntity?
}
