package com.kyovo.provider

import com.kyovo.domain.port.secondary.ClockPort
import java.time.OffsetDateTime

class SystemTimeProvider : ClockPort
{
    override fun now(): OffsetDateTime
    {
        return OffsetDateTime.now()
    }
}
