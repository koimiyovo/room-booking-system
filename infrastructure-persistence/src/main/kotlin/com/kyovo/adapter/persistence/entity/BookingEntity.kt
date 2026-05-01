package com.kyovo.adapter.persistence.entity

import com.kyovo.domain.model.*
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "bookings")
class BookingEntity(
    @Id
    val id: UUID,

    @Column(nullable = false)
    val roomId: UUID,

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val startDate: LocalDate,

    @Column(nullable = false)
    val endDate: LocalDate,

    @Column(nullable = false)
    val numberOfPeople: Int,

    @Column(nullable = true)
    val specialRequests: String?,

    @Column(nullable = false)
    val status: String,

    @Column(nullable = true)
    val cancellationReason: String?,

    @Column(nullable = true)
    val cancelledBy: UUID?
)
{
    companion object
    {
        fun fromDomain(booking: Booking): BookingEntity
        {
            return BookingEntity(
                id = booking.id.value,
                roomId = booking.roomId.value,
                userId = booking.userId.value,
                startDate = booking.startDate.value,
                endDate = booking.endDate.value,
                numberOfPeople = booking.numberOfPeople.value,
                specialRequests = booking.specialRequests?.value,
                status = booking.status.label,
                cancellationReason = booking.cancellation?.reason?.value,
                cancelledBy = booking.cancellation?.cancelledBy?.value
            )
        }
    }

    fun toDomain(): Booking
    {
        val cancellation = cancelledBy?.let {
            Cancellation(
                cancelledBy = UserId(it),
                reason = cancellationReason?.let { r -> BookingCancellationReason(r) }
            )
        }
        return Booking(
            id = BookingId(id),
            roomId = RoomId(roomId),
            userId = UserId(userId),
            startDate = BookingStartDate(startDate),
            endDate = BookingEndDate(endDate),
            numberOfPeople = BookingNumberOfPeople(numberOfPeople),
            specialRequests = specialRequests?.let { BookingSpecialRequests(it) },
            cancellation = cancellation
        )
    }
}
