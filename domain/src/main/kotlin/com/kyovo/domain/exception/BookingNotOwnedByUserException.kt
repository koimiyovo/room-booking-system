package com.kyovo.domain.exception

import com.kyovo.domain.model.BookingId
import com.kyovo.domain.model.UserId

class BookingNotOwnedByUserException(bookingId: BookingId, userId: UserId) :
    RuntimeException("Booking ${bookingId.value} does not belong to user ${userId.value}")
