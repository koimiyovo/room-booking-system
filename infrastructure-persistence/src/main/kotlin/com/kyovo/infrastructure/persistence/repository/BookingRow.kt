package com.kyovo.infrastructure.persistence.repository

import com.kyovo.domain.model.booking.Booking
import com.kyovo.infrastructure.persistence.entity.BookingEntity
import com.kyovo.infrastructure.persistence.entity.BookingStatusHistoryEntity

data class BookingRow(val booking: BookingEntity, val status: BookingStatusHistoryEntity)
{
    fun toBooking(): Booking = booking.toDomain(status.toStatusInfo())
}
