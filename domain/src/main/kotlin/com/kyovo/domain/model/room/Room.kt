package com.kyovo.domain.model.room

import com.kyovo.domain.model.user.UserId

data class Room(
    val id: RoomId,
    val name: RoomName,
    val capacity: RoomCapacity,
    val requiresValidation: Boolean,
    val createdBy: UserId
)
