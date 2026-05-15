package com.kyovo.domain.port.secondary

import com.kyovo.domain.model.booking.Booking
import com.kyovo.domain.model.user.UserEmail

interface NotificationPort
{
    fun sendBookingStatusNotification(userEmail: UserEmail, booking: Booking)
}
