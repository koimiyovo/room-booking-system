package com.kyovo.provider

import com.kyovo.domain.provider.TimeProvider
import java.time.Clock
import java.time.LocalDateTime

class SystemTimeProvider(private val clock: Clock) : TimeProvider
{
    override fun now(): LocalDateTime
    {
        return LocalDateTime.now(clock)
    }
}