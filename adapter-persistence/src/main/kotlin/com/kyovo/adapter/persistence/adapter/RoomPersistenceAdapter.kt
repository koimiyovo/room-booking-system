package com.kyovo.adapter.persistence.adapter

import com.kyovo.adapter.persistence.entity.RoomEntity
import com.kyovo.adapter.persistence.repository.RoomJpaRepository
import com.kyovo.domain.model.*
import com.kyovo.domain.port.secondary.RoomRepository
import org.springframework.stereotype.Component

@Component
class RoomPersistenceAdapter(private val jpaRepository: RoomJpaRepository) : RoomRepository
{
    override fun findAll(): List<Room>
    {
        return jpaRepository.findAll().map { it.toDomain() }
    }

    override fun findById(id: RoomId): Room?
    {
        return jpaRepository.findById(id.value).orElse(null)?.toDomain()
    }

    override fun save(room: Room): Room
    {
        val entity = RoomEntity.fromDomain(room)
        return jpaRepository.save(entity).toDomain()
    }
}
