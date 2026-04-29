package com.kyovo.domain.model

import com.fasterxml.uuid.Generators
import java.util.UUID

@JvmInline
value class BookingId(val value: UUID)
{
    companion object
    {
        fun generate(): BookingId
        {
            return BookingId(Generators.timeBasedEpochGenerator().generate())
        }
    }
}
