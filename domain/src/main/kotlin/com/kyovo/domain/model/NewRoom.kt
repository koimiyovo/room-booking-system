package com.kyovo.domain.model

data class NewRoom(
    val name: RoomName,
    val capacity: RoomCapacity
)
{
    fun toRoom(): Room
    {
        return Room(RoomId.generate(), name, capacity)
    }
}
