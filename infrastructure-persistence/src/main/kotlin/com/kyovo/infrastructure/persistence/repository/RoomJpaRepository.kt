package com.kyovo.infrastructure.persistence.repository

import com.kyovo.infrastructure.persistence.entity.RoomEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface RoomJpaRepository : JpaRepository<RoomEntity, UUID>
{
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RoomEntity r WHERE r.id = :id")
    fun findByIdForBooking(@Param("id") id: UUID): RoomEntity?
}
