package com.kyovo.domain.port.primary

import com.kyovo.domain.model.Booking
import com.kyovo.domain.model.BookingId
import com.kyovo.domain.model.NewBooking

interface BookingUseCase
{
    fun findAll(): List<Booking>

    fun findById(id: BookingId): Booking?

    fun create(newBooking: NewBooking): Booking
}
