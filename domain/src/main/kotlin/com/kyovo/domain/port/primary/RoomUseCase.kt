package com.kyovo.domain.port.primary

import com.kyovo.domain.model.room.NewRoom
import com.kyovo.domain.model.room.Room
import com.kyovo.domain.model.room.RoomId

interface RoomUseCase
{
    fun findAll(): List<Room>

    fun findById(id: RoomId): Room?

    fun save(newRoom: NewRoom): Room
}
