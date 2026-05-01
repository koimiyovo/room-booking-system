package com.kyovo.infrastructure.provider

import com.kyovo.domain.port.secondary.ClockPort
import java.time.OffsetDateTime

class MutableTimeProvider(private var current: OffsetDateTime) : ClockPort
{
    override fun now(): OffsetDateTime
    {
        return current
    }

    fun setNow(date: OffsetDateTime)
    {
        this.current = date
    }
}