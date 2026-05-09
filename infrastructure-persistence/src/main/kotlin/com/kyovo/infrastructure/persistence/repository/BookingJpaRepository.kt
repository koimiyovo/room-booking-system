package com.kyovo.infrastructure.persistence.repository

import com.kyovo.infrastructure.persistence.entity.BookingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.*

private const val SELECT_BOOKING_ROW =
    "SELECT new com.kyovo.infrastructure.persistence.repository.BookingRow(b, h)" +
            " FROM BookingEntity b JOIN BookingStatusHistoryEntity h ON h.booking.id = b.id WHERE h.until IS NULL"

interface BookingJpaRepository : JpaRepository<BookingEntity, UUID>
{
    @Query(SELECT_BOOKING_ROW)
    fun findAllWithCurrentStatus(): List<BookingRow>

    @Query("$SELECT_BOOKING_ROW AND b.id = :id")
    fun findByIdWithCurrentStatus(@Param("id") id: UUID): List<BookingRow>

    @Query("$SELECT_BOOKING_ROW AND b.user.id = :userId")
    fun findByUserId(@Param("userId") userId: UUID): List<BookingRow>

    @Query(
        """
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM BookingEntity b JOIN BookingStatusHistoryEntity h ON h.booking.id = b.id
        WHERE h.until IS NULL
          AND h.status = 'CONFIRMED'
          AND b.room.id = :roomId
          AND b.startDate < :endDate
          AND b.endDate > :startDate
    """
    )
    fun existsOverlap(
        @Param("roomId") roomId: UUID,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): Boolean

    @Query(
        """
        SELECT new com.kyovo.infrastructure.persistence.repository.BookingRow(b, h)
        FROM BookingEntity b JOIN BookingStatusHistoryEntity h ON h.booking.id = b.id
        WHERE h.until IS NULL
          AND h.status = 'PENDING'
          AND b.room.id = :roomId
          AND b.startDate < :endDate
          AND b.endDate > :startDate
          AND b.id <> :excludeId
    """
    )
    fun findOverlappingPending(
        @Param("roomId") roomId: UUID,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("excludeId") excludeId: UUID
    ): List<BookingRow>
}
