package com.kyovo.domain.provider

import java.time.OffsetDateTime

interface TimeProvider
{
    fun now(): OffsetDateTime
}