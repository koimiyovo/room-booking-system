package com.kyovo.infrastructure.provider

import com.kyovo.domain.model.booking.Booking
import com.kyovo.domain.model.booking.BookingStatus.*
import com.kyovo.domain.model.user.UserEmail
import com.kyovo.domain.model.user.UserStatus
import com.kyovo.domain.model.user.UserStatus.*
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

    override fun sendUserStatusNotification(userEmail: UserEmail, status: UserStatus)
    {
        val message = SimpleMailMessage()
        message.from = from
        message.setTo(userEmail.value)

        when (status)
        {
            CREATED ->
            {
                message.subject = "Welcome – your account has been created"
                message.text = """
                    Your account has been successfully created.

                    You can now log in and start booking rooms.
                """.trimIndent()
            }

            INACTIVE ->
            {
                message.subject = "Your account has been deactivated"
                message.text = """
                    Your account has been deactivated.

                    You will not be able to make new bookings until your account is reactivated.
                    Please contact support if you think this is a mistake.
                """.trimIndent()
            }

            ACTIVE ->
            {
                message.subject = "Your account has been reactivated"
                message.text = """
                    Your account has been reactivated.

                    You can now log in and start booking rooms again.
                """.trimIndent()
            }

            DELETED ->
            {
                message.subject = "Your account has been deleted"
                message.text = """
                    Your account has been permanently deleted.

                    All your data has been removed. If this was not intentional, please contact support.
                """.trimIndent()
            }
        }

        mailSender.send(message)
    }

    override fun sendUserProfileUpdatedNotification(userEmail: UserEmail)
    {
        val message = SimpleMailMessage()
        message.from = from
        message.setTo(userEmail.value)
        message.subject = "Your account information has been updated"
        message.text = """
            Your account information has been successfully updated.

            If you did not make this change, please contact support immediately.
        """.trimIndent()
        mailSender.send(message)
    }
}
