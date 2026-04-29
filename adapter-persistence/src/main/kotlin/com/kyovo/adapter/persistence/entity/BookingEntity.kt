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
    val specialRequests: String?
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
                specialRequests = booking.specialRequests?.value
            )
        }
    }

    fun toDomain(): Booking
    {
        return Booking(
            id = BookingId(id),
            roomId = RoomId(roomId),
            userId = UserId(userId),
            startDate = BookingStartDate(startDate),
            endDate = BookingEndDate(endDate),
            numberOfPeople = BookingNumberOfPeople(numberOfPeople),
            specialRequests = specialRequests?.let { BookingSpecialRequests(it) }
        )
    }
}
