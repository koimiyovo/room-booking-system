package com.kyovo.infrastructure.persistence.adapter

import com.kyovo.domain.model.booking.Booking
import com.kyovo.domain.model.booking.BookingEndDate
import com.kyovo.domain.model.booking.BookingId
import com.kyovo.domain.model.booking.BookingStartDate
import com.kyovo.domain.model.room.RoomId
import com.kyovo.domain.model.user.UserId
import com.kyovo.domain.port.secondary.BookingRepository
import com.kyovo.infrastructure.persistence.entity.BookingEntity
import com.kyovo.infrastructure.persistence.repository.BookingJpaRepository
import com.kyovo.infrastructure.persistence.repository.RoomJpaRepository
import com.kyovo.infrastructure.persistence.repository.UserJpaRepository
import org.springframework.stereotype.Component

@Component
class BookingPersistenceAdapter(
    private val jpaRepository: BookingJpaRepository,
    private val roomJpaRepository: RoomJpaRepository,
    private val userJpaRepository: UserJpaRepository
) : BookingRepository
{
    override fun findAll(): List<Booking>
    {
        return jpaRepository.findAll().map { it.toDomain() }
    }

    override fun findById(id: BookingId): Booking?
    {
        return jpaRepository.findById(id.value).orElse(null)?.toDomain()
    }

    override fun findByUserId(userId: UserId): List<Booking>
    {
        return jpaRepository.findByUserId(userId.value).map { it.toDomain() }
    }

    override fun save(booking: Booking): Booking
    {
        val room = roomJpaRepository.getReferenceById(booking.roomId.value)
        val user = userJpaRepository.getReferenceById(booking.userId.value)
        val cancelledByUser = booking.cancellation?.cancelledBy?.let { userJpaRepository.getReferenceById(it.value) }
        val entity = BookingEntity.fromDomain(booking, room, user, cancelledByUser)
        return jpaRepository.save(entity).toDomain()
    }

    override fun update(booking: Booking): Booking
    {
        val room = roomJpaRepository.getReferenceById(booking.roomId.value)
        val user = userJpaRepository.getReferenceById(booking.userId.value)
        val cancelledByUser = booking.cancellation?.cancelledBy?.let { userJpaRepository.getReferenceById(it.value) }
        val entity = BookingEntity.fromDomain(booking, room, user, cancelledByUser)
        return jpaRepository.save(entity).toDomain()
    }

    override fun existsOverlappingBooking(
        roomId: RoomId,
        startDate: BookingStartDate,
        endDate: BookingEndDate
    ): Boolean
    {
        return jpaRepository.existsOverlap(roomId.value, startDate.value, endDate.value)
    }
}
