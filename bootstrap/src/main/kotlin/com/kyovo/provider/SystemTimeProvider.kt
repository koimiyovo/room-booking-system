package com.kyovo.provider

import com.kyovo.domain.provider.TimeProvider
import java.time.Clock
import java.time.OffsetDateTime

class SystemTimeProvider(private val clock: Clock) : TimeProvider
{
    override fun now(): OffsetDateTime
    {
        return OffsetDateTime.now(clock)
    }
}