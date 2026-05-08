package com.kyovo.infrastructure.api.dto

import com.kyovo.domain.model.room.NewRoom
import com.kyovo.domain.model.room.RoomCapacity
import com.kyovo.domain.model.room.RoomName
import com.kyovo.domain.model.user.UserId

data class CreateRoomRequest(
    val name: String,
    val capacity: Int,
    val requiresValidation: Boolean
)
{
    fun toNewRoom(createdBy: UserId): NewRoom
    {
        return NewRoom(
            name = RoomName(name),
            capacity = RoomCapacity(capacity),
            requiresValidation = requiresValidation,
            createdBy = createdBy
        )
    }
}
