package com.kyovo.domain.port.secondary

import com.kyovo.domain.model.*

interface RoomRepository
{
    fun findAll(): List<Room>

    fun findById(id: RoomId): Room?

    fun save(room: Room): Room
}
