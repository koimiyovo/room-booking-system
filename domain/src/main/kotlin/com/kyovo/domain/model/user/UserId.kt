package com.kyovo.domain.model.user

import com.fasterxml.uuid.Generators
import java.util.*

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
