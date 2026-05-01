package com.kyovo.domain.exception

import com.kyovo.domain.model.BookingId

class BookingAlreadyCancelledException(id: BookingId) :
    RuntimeException("Booking ${id.value} is already cancelled")
