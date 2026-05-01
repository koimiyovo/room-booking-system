package com.kyovo.adapter.persistence.repository

import com.kyovo.adapter.persistence.entity.BookingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface BookingJpaRepository : JpaRepository<BookingEntity, UUID>
{
    fun findByUserId(userId: UUID): List<BookingEntity>

    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM BookingEntity b
        WHERE b.roomId = :roomId
          AND b.startDate < :endDate
          AND b.endDate > :startDate
          AND b.status = 'CONFIRMED'
    """)
    fun existsOverlap(
        @Param("roomId") roomId: UUID,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): Boolean
}
