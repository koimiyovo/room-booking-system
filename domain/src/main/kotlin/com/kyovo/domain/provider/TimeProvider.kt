package com.kyovo.domain.provider

import java.time.LocalDateTime

interface TimeProvider
{
    fun now(): LocalDateTime
}