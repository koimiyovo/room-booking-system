package com.kyovo.infrastructure.persistence.adapter

import com.kyovo.domain.model.booking.*
import com.kyovo.domain.model.room.RoomId
import com.kyovo.domain.model.user.UserId
import com.kyovo.domain.port.secondary.BookingRepository
import com.kyovo.domain.port.secondary.TransactionPort
import com.kyovo.infrastructure.persistence.entity.BookingEntity
import com.kyovo.infrastructure.persistence.entity.BookingStatusHistoryEntity
import com.kyovo.infrastructure.persistence.repository.BookingJpaRepository
import com.kyovo.infrastructure.persistence.repository.BookingStatusHistoryJpaRepository
import com.kyovo.infrastructure.persistence.repository.RoomJpaRepository
import com.kyovo.infrastructure.persistence.repository.UserJpaRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class BookingPersistenceAdapter(
    private val jpaRepository: BookingJpaRepository,
    private val roomJpaRepository: RoomJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val statusHistoryJpaRepository: BookingStatusHistoryJpaRepository,
    private val transactionPort: TransactionPort
) : BookingRepository
{
    override fun findAll(): List<Booking>
    {
        return jpaRepository.findAllWithCurrentStatus().map { it.toBooking() }
    }

    override fun findById(id: BookingId): Booking?
    {
        return jpaRepository.findByIdWithCurrentStatus(id.value).firstOrNull()?.toBooking()
    }

    override fun findByUserId(userId: UserId): List<Booking>
    {
        return jpaRepository.findByUserId(userId.value).map { it.toBooking() }
    }

    override fun save(booking: Booking): Booking
    {
        val room = roomJpaRepository.getReferenceById(booking.roomId.value)
        val user = userJpaRepository.getReferenceById(booking.userId.value)
        val entity = BookingEntity.fromDomain(booking, room, user)
        return jpaRepository.save(entity).toDomain(booking.statusInfo)
    }

    override fun update(booking: Booking): Booking
    {
        val room = roomJpaRepository.getReferenceById(booking.roomId.value)
        val user = userJpaRepository.getReferenceById(booking.userId.value)
        val entity = BookingEntity.fromDomain(booking, room, user)
        return jpaRepository.save(entity).toDomain(booking.statusInfo)
    }

    override fun existsOverlappingBooking(
        roomId: RoomId,
        startDate: BookingStartDate,
        endDate: BookingEndDate
    ): Boolean
    {
        return jpaRepository.existsOverlap(roomId.value, startDate.value, endDate.value)
    }

    override fun saveStatusHistory(
        bookingId: BookingId,
        status: BookingStatus,
        changedAt: BookingStatusHistoryDate,
        changedBy: UserId?,
        reason: BookingStatusReason?
    )
    {
        transactionPort.executeInTransaction {
            statusHistoryJpaRepository.closeCurrentEntry(bookingId.value, changedAt.value)
            val bookingRef = jpaRepository.getReferenceById(bookingId.value)
            val changedByRef = changedBy?.let { userJpaRepository.getReferenceById(it.value) }
            statusHistoryJpaRepository.save(
                BookingStatusHistoryEntity(
                    id = UUID.randomUUID(),
                    booking = bookingRef,
                    status = status.label,
                    changedAt = changedAt.value,
                    changedByUser = changedByRef,
                    reason = reason?.value,
                    until = null
                )
            )
        }
    }

    override fun findStatusHistory(bookingId: BookingId): List<BookingStatusHistory>
    {
        return statusHistoryJpaRepository.findAllByBookingId(bookingId.value).map { it.toDomain() }
    }

    override fun findOverlappingPendingBookings(
        roomId: RoomId,
        startDate: BookingStartDate,
        endDate: BookingEndDate,
        excludeBookingId: BookingId
    ): List<Booking>
    {
        return jpaRepository.findOverlappingPending(roomId.value, startDate.value, endDate.value, excludeBookingId.value)
            .map { it.toBooking() }
    }
}
