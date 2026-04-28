package com.kyovo.domain.service

import com.kyovo.domain.model.NewRoom
import com.kyovo.domain.model.Room
import com.kyovo.domain.model.RoomId
import com.kyovo.domain.port.primary.RoomUseCase
import com.kyovo.domain.port.secondary.RoomRepository

class RoomService(private val roomRepository: RoomRepository) : RoomUseCase {
    override fun findAll(): List<Room> {
        return roomRepository.findAll()
    }

    override fun findById(id: RoomId): Room? {
        return roomRepository.findById(id)
    }

    override fun save(newRoom: NewRoom): Room {
        val room = Room(
            id = RoomId.generate(),
            name = newRoom.name,
            capacity = newRoom.capacity
        )
        return roomRepository.save(room)
    }
}
