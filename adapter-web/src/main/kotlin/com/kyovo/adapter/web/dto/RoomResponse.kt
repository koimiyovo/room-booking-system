package com.kyovo.adapter.web.dto

import com.kyovo.domain.model.Room
import java.util.*

data class RoomResponse(
    val id: UUID,
    val name: String,
    val capacity: Int
)
{
    companion object
    {
        fun fromDomain(room: Room): RoomResponse
        {
            return RoomResponse(
                id = room.id.value,
                name = room.name.value,
                capacity = room.capacity.value
            )
        }
    }
}
