package com.kyovo.domain.model

import java.time.LocalDateTime
import java.time.OffsetDateTime

@JvmInline
value class UserRegistrationDate(val value: OffsetDateTime)
{
    companion object
    {
        fun fromLocalDateTime(localDateTime: LocalDateTime): UserRegistrationDate
        {
            return UserRegistrationDate(localDateTime.atOffset(OffsetDateTime.now().offset))
        }
    }
}
