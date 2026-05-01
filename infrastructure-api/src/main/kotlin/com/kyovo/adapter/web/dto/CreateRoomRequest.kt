package com.kyovo.adapter.web.dto

import com.kyovo.domain.model.*

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
