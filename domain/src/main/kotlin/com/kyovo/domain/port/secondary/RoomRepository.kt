package com.kyovo.domain.port.secondary

import com.kyovo.domain.model.Room
import com.kyovo.domain.model.RoomId

interface RoomRepository
{
    fun findAll(): List<Room>

    fun findById(id: RoomId): Room?

    fun findByIdForBooking(id: RoomId): Room?

    fun save(room: Room): Room
}
