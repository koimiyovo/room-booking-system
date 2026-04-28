package com.kyovo.domain.port.primary

import com.kyovo.domain.model.*

interface RoomUseCase
{
    fun findAll(): List<Room>

    fun findById(id: RoomId): Room?

    fun save(newRoom: NewRoom): Room
}
