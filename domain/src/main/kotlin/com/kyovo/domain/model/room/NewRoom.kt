package com.kyovo.domain.model.room

import com.kyovo.domain.model.user.UserId

data class NewRoom(
    val name: RoomName,
    val capacity: RoomCapacity,
    val requiresValidation: Boolean,
    val createdBy: UserId
)
{
    fun toRoom(): Room
    {
        return Room(RoomId.generate(), name, capacity, requiresValidation, createdBy)
    }
}
