package com.kyovo.domain.model.room

data class NewRoom(
    val name: RoomName,
    val capacity: RoomCapacity,
    val requiresValidation: Boolean
)
{
    fun toRoom(): Room
    {
        return Room(RoomId.generate(), name, capacity, requiresValidation)
    }
}
