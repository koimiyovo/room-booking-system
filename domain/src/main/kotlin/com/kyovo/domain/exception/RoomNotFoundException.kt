package com.kyovo.domain.exception

import com.kyovo.domain.model.room.RoomId

class RoomNotFoundException(val roomId: RoomId) : RuntimeException("Room not found: ${roomId.value}")
