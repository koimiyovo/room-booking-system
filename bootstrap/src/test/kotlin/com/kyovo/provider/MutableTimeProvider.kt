package com.kyovo.provider

import com.kyovo.domain.provider.TimeProvider
import java.time.OffsetDateTime

class MutableTimeProvider(private var current: OffsetDateTime) : TimeProvider
{
    override fun now(): OffsetDateTime
    {
        return current
    }

    fun setNow(newNow: OffsetDateTime)
    {
        this.current = newNow
    }
}