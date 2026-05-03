package com.kyovo.infrastructure.persistence.repository

import com.kyovo.infrastructure.persistence.entity.UserStatusHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

interface UserStatusHistoryJpaRepository : JpaRepository<UserStatusHistoryEntity, UUID>
{
    @Modifying
    @Transactional
    @Query("UPDATE UserStatusHistoryEntity e SET e.until = :until WHERE e.user.id = :userId AND e.until IS NULL")
    fun closeCurrentEntry(@Param("userId") userId: UUID, @Param("until") until: OffsetDateTime)

    @Query("SELECT h FROM UserStatusHistoryEntity h WHERE h.user.id = :userId")
    fun findAllByUserId(@Param("userId") userId: UUID): List<UserStatusHistoryEntity>

    @Query("SELECT h FROM UserStatusHistoryEntity h WHERE h.user.id = :userId AND h.until IS NULL")
    fun findByUserIdAndUntilIsNull(@Param("userId") userId: UUID): UserStatusHistoryEntity?
}
