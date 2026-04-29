package com.kyovo.adapter.persistence.repository

import com.kyovo.adapter.persistence.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserJpaRepository : JpaRepository<UserEntity, UUID>
{
    fun findByEmail(email: String): UserEntity?
}
