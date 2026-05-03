package com.kyovo.infrastructure.persistence.entity

import com.kyovo.domain.model.booking.*
import com.kyovo.domain.model.room.RoomId
import com.kyovo.domain.model.user.UserId
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "bookings")
class BookingEntity(
    @Id
    val id: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, foreignKey = ForeignKey(name = "fk_booking_room_id"))
    val room: RoomEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = ForeignKey(name = "fk_booking_user_id"))
    val user: UserEntity,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    val endDate: LocalDate,

    @Column(name = "number_of_people", nullable = false)
    val numberOfPeople: Int,

    @Column(name = "special_requests", nullable = true)
    val specialRequests: String?,

    @Column(nullable = false)
    val status: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "cancelled_by", nullable = true, foreignKey = ForeignKey(name = "fk_booking_cancelled_by"))
    val cancelledByUser: UserEntity?,

    @Column(name = "cancellation_reason", nullable = true)
    val cancellationReason: String?
)
{
    companion object
    {
        fun fromDomain(
            booking: Booking,
            room: RoomEntity,
            user: UserEntity,
            cancelledByUser: UserEntity?
        ): BookingEntity
        {
            return BookingEntity(
                id = booking.id.value,
                room = room,
                user = user,
                startDate = booking.startDate.value,
                endDate = booking.endDate.value,
                numberOfPeople = booking.numberOfPeople.value,
                specialRequests = booking.specialRequests?.value,
                status = booking.status.label,
                cancellationReason = booking.cancellation?.reason?.value,
                cancelledByUser = cancelledByUser
            )
        }
    }

    fun toDomain(): Booking
    {
        val cancellation = cancelledByUser?.let {
            Cancellation(
                cancelledBy = UserId(it.id),
                reason = cancellationReason?.let { r -> BookingCancellationReason(r) }
            )
        }
        return Booking(
            id = BookingId(id),
            roomId = RoomId(room.id),
            userId = UserId(user.id),
            startDate = BookingStartDate(startDate),
            endDate = BookingEndDate(endDate),
            numberOfPeople = BookingNumberOfPeople(numberOfPeople),
            specialRequests = specialRequests?.let { BookingSpecialRequests(it) },
            cancellation = cancellation
        )
    }
}
