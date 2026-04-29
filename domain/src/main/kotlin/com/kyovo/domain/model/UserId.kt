package com.kyovo.domain.model

import com.fasterxml.uuid.Generators
import java.util.UUID

@JvmInline
value class UserId(val value: UUID)
{
    companion object
    {
        fun generate(): UserId
        {
            return UserId(Generators.timeBasedEpochGenerator().generate())
        }
    }
}
