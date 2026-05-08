package com.kyovo.infrastructure.api.dto

import com.kyovo.domain.model.booking.BookingStatus

enum class BookingStatusResponse
{
    PENDING,
    CONFIRMED,
    CANCELED;

    companion object
    {
        fun from(status: BookingStatus): BookingStatusResponse
        {
            return when (status)
            {
                BookingStatus.PENDING -> PENDING
                BookingStatus.CONFIRMED -> CONFIRMED
                BookingStatus.CANCELED -> CANCELED
            }
        }
    }
}
