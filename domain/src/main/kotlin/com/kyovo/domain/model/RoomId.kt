package com.kyovo.domain.model

import com.fasterxml.uuid.Generators
import java.util.UUID

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
