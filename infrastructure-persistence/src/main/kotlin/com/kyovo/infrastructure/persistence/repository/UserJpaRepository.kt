package com.kyovo.infrastructure.persistence.repository

import com.kyovo.infrastructure.persistence.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

private const val SELECT_USER_ROW =
    "SELECT new com.kyovo.infrastructure.persistence.repository.UserRow(u, h)" +
            " FROM UserEntity u JOIN UserStatusHistoryEntity h ON h.userId = u.id WHERE h.until IS NULL"

private const val SELECT_NON_DELETED_USER_ROW = "$SELECT_USER_ROW AND h.status <> 'DELETED'"

interface UserJpaRepository : JpaRepository<UserEntity, UUID>
{
    @Query(SELECT_USER_ROW)
    fun findAllWithCurrentStatus(): List<UserRow>

    @Query(SELECT_NON_DELETED_USER_ROW + " AND u.id = :id")
    fun findByIdWithCurrentStatus(@Param("id") id: UUID): List<UserRow>

    @Query(SELECT_NON_DELETED_USER_ROW + " AND u.email = :email")
    fun findByEmailWithCurrentStatus(@Param("email") email: String): List<UserRow>
}
