package com.kyovo.infrastructure.persistence.repository

import com.kyovo.infrastructure.persistence.entity.BookingStatusHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface BookingStatusHistoryJpaRepository : JpaRepository<BookingStatusHistoryEntity, UUID>
{
    @Query("SELECT h FROM BookingStatusHistoryEntity h WHERE h.booking.id = :bookingId ORDER BY h.changedAt ASC")
    fun findAllByBookingId(@Param("bookingId") bookingId: UUID): List<BookingStatusHistoryEntity>
}
