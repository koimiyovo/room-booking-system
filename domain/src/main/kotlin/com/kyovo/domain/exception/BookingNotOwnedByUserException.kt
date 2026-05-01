package com.kyovo.domain.exception

import com.kyovo.domain.model.booking.BookingId
import com.kyovo.domain.model.user.UserId

class BookingNotOwnedByUserException(bookingId: BookingId, userId: UserId) :
    RuntimeException("Booking ${bookingId.value} does not belong to user ${userId.value}")
