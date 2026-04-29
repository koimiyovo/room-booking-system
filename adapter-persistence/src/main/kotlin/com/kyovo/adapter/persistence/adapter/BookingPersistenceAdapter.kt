package com.kyovo.adapter.persistence.adapter

import com.kyovo.adapter.persistence.entity.BookingEntity
import com.kyovo.adapter.persistence.repository.BookingJpaRepository
import com.kyovo.domain.model.*
import com.kyovo.domain.port.secondary.BookingRepository
import org.springframework.stereotype.Component

@Component
class BookingPersistenceAdapter(private val jpaRepository: BookingJpaRepository) : BookingRepository
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
        val entity = BookingEntity.fromDomain(booking)
        return jpaRepository.save(entity).toDomain()
    }

    override fun update(booking: Booking): Booking
    {
        val entity = BookingEntity.fromDomain(booking)
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
