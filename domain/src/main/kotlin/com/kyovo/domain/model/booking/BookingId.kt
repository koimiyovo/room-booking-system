package com.kyovo.domain.model.booking

import com.fasterxml.uuid.Generators
import java.util.*

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
