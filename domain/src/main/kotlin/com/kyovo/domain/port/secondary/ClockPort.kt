package com.kyovo.domain.port.secondary

import java.time.OffsetDateTime

interface ClockPort
{
    fun now(): OffsetDateTime
}
