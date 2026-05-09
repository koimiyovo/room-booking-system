package com.kyovo.infrastructure.persistence.repository

import com.kyovo.infrastructure.persistence.entity.BookingStatusHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*

interface BookingStatusHistoryJpaRepository : JpaRepository<BookingStatusHistoryEntity, UUID>
{
    @Modifying
    @Transactional
    @Query("UPDATE BookingStatusHistoryEntity e SET e.until = :until WHERE e.booking.id = :bookingId AND e.until IS NULL")
    fun closeCurrentEntry(@Param("bookingId") bookingId: UUID, @Param("until") until: OffsetDateTime)

    @Query("SELECT h FROM BookingStatusHistoryEntity h WHERE h.booking.id = :bookingId ORDER BY h.changedAt ASC")
    fun findAllByBookingId(@Param("bookingId") bookingId: UUID): List<BookingStatusHistoryEntity>
}
