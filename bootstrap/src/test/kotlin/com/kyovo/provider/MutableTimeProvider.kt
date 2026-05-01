package com.kyovo.provider

import com.kyovo.domain.provider.TimeProvider
import java.time.LocalDateTime

class MutableTimeProvider(private var current: LocalDateTime) : TimeProvider
{
    override fun now(): LocalDateTime
    {
        return current
    }

    fun setNow(newNow: LocalDateTime)
    {
        this.current = newNow
    }
}