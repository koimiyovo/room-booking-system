package com.kyovo.infrastructure.provider

import com.kyovo.domain.model.booking.Booking
import com.kyovo.domain.model.booking.BookingStatus.CANCELED
import com.kyovo.domain.model.booking.BookingStatus.CONFIRMED
import com.kyovo.domain.model.booking.BookingStatus.PENDING
import com.kyovo.domain.model.user.UserEmail
import com.kyovo.domain.port.secondary.NotificationPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class EmailNotificationAdapter(
    private val mailSender: JavaMailSender,
    @param:Value($$"${app.mail.from}") private val from: String
) : NotificationPort
{
    override fun sendBookingStatusNotification(userEmail: UserEmail, booking: Booking)
    {
        val message = SimpleMailMessage()
        message.from = from
        message.setTo(userEmail.value)

        when (booking.status)
        {
            PENDING ->
            {
                message.subject = "Booking request received – pending confirmation"
                message.text = """
                    Your booking request has been received and is pending confirmation.

                    Booking details:
                    - From: ${booking.startDate.value}
                    - To: ${booking.endDate.value}
                    - Guests: ${booking.numberOfPeople.value}

                    We will notify you once your booking is confirmed.
                """.trimIndent()
            }

            CONFIRMED ->
            {
                message.subject = "Booking confirmed"
                message.text = """
                    Your booking has been confirmed!

                    Booking details:
                    - From: ${booking.startDate.value}
                    - To: ${booking.endDate.value}
                    - Guests: ${booking.numberOfPeople.value}

                    Thank you for your booking.
                """.trimIndent()
            }

            CANCELED ->
            {
                message.subject = "Booking cancelled"
                message.text = """
                    Your booking has been cancelled.

                    Booking details:
                    - From: ${booking.startDate.value}
                    - To: ${booking.endDate.value}
                    - Guests: ${booking.numberOfPeople.value}
                """.trimIndent()
            }
        }

        mailSender.send(message)
    }
}
