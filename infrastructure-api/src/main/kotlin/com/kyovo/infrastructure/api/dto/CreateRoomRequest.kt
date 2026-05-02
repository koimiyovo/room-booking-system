package com.kyovo.infrastructure.api.dto

import com.kyovo.domain.model.room.NewRoom
import com.kyovo.domain.model.room.RoomCapacity
import com.kyovo.domain.model.room.RoomName

data class CreateRoomRequest(
    val name: String,
    val capacity: Int
)
{
    fun toNewRoom(): NewRoom
    {
        return NewRoom(
            name = RoomName(name),
            capacity = RoomCapacity(capacity)
        )
    }
}
