package com.kyovo.domain.exception

import com.kyovo.domain.model.booking.BookingId

class BookingNotPendingException(id: BookingId) : RuntimeException("Booking ${id.value} is not pending")
