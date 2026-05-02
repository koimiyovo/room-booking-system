package com.kyovo.infrastructure.persistence.repository

import com.kyovo.infrastructure.persistence.entity.UserStatusHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

interface UserStatusHistoryJpaRepository : JpaRepository<UserStatusHistoryEntity, UUID>
{
    @Modifying
    @Transactional
    @Query("UPDATE UserStatusHistoryEntity e SET e.until = :until WHERE e.userId = :userId AND e.until IS NULL")
    fun closeCurrentEntry(userId: UUID, until: OffsetDateTime)

    fun findAllByUserId(userId: UUID): List<UserStatusHistoryEntity>

    fun findByUserIdAndUntilIsNull(userId: UUID): UserStatusHistoryEntity?
}
