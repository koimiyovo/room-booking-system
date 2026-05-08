package com.kyovo.infrastructure.persistence.adapter

import com.kyovo.domain.model.room.Room
import com.kyovo.domain.model.room.RoomId
import com.kyovo.domain.port.secondary.RoomRepository
import com.kyovo.infrastructure.persistence.entity.RoomEntity
import com.kyovo.infrastructure.persistence.repository.RoomJpaRepository
import com.kyovo.infrastructure.persistence.repository.UserJpaRepository
import org.springframework.stereotype.Component

@Component
class RoomPersistenceAdapter(
    private val jpaRepository: RoomJpaRepository,
    private val userJpaRepository: UserJpaRepository
) : RoomRepository
{
    override fun findAll(): List<Room>
    {
        return jpaRepository.findAll().map { it.toDomain() }
    }

    override fun findById(id: RoomId): Room?
    {
        return jpaRepository.findById(id.value).orElse(null)?.toDomain()
    }

    override fun findByIdForBooking(id: RoomId): Room?
    {
        return jpaRepository.findByIdForBooking(id.value)?.toDomain()
    }

    override fun save(room: Room): Room
    {
        val createdByUser = userJpaRepository.getReferenceById(room.createdBy.value)
        val entity = RoomEntity.fromDomain(room, createdByUser)
        return jpaRepository.save(entity).toDomain()
    }
}
