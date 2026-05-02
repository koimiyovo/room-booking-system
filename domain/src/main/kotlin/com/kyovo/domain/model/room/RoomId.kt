package com.kyovo.domain.model.room

import com.fasterxml.uuid.Generators
import java.util.*

@JvmInline
value class RoomId(val value: UUID)
{
    companion object
    {
        fun generate(): RoomId
        {
            return RoomId(Generators.timeBasedEpochGenerator().generate())
        }
    }
}
