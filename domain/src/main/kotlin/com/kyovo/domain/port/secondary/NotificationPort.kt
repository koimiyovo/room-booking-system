package com.kyovo.domain.port.secondary

import com.kyovo.domain.model.booking.Booking
import com.kyovo.domain.model.user.UserEmail
import com.kyovo.domain.model.user.UserStatus

interface NotificationPort
{
    fun sendBookingStatusNotification(userEmail: UserEmail, booking: Booking)
    fun sendUserStatusNotification(userEmail: UserEmail, status: UserStatus)
    fun sendUserProfileUpdatedNotification(userEmail: UserEmail)
}
