package com.kyovo.domain.exception

import com.kyovo.domain.model.booking.BookingId

class BookingNotFoundException(id: BookingId) : RuntimeException("Booking not found: ${id.value}")
