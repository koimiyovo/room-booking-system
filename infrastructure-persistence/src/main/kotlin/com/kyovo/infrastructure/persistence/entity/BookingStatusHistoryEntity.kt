package com.kyovo.infrastructure.persistence.entity

import com.kyovo.domain.model.booking.*
import com.kyovo.domain.model.user.UserId
import com.kyovo.infrastructure.persistence.exception.InvalidBookingStatusException
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "booking_status_history")
class BookingStatusHistoryEntity(
    @Id
    val id: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, foreignKey = ForeignKey(name = "fk_bsh_booking_id"))
    val booking: BookingEntity,

    @Column(nullable = false)
    val status: String,

    @Column(name = "changed_at", nullable = false)
    val changedAt: OffsetDateTime,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "changed_by", nullable = true, foreignKey = ForeignKey(name = "fk_bsh_changed_by"))
    val changedByUser: UserEntity?,

    @Column(nullable = true)
    val reason: String?,

    @Column(nullable = true)
    val until: OffsetDateTime?
)
{
    fun toStatusInfo(): BookingStatusInfo
    {
        val parsedStatus = BookingStatus.entries.firstOrNull { it.label == status }
            ?: throw InvalidBookingStatusException(status)
        return BookingStatusInfo(
            status = parsedStatus,
            since = BookingStatusInfoDate(changedAt),
            changedBy = changedByUser?.let { UserId(it.id) },
            reason = reason?.let { BookingStatusReason(it) }
        )
    }

    fun toDomain(): BookingStatusHistory
    {
        val parsedStatus = BookingStatus.entries.firstOrNull { it.label == status }
            ?: throw InvalidBookingStatusException(status)
        return BookingStatusHistory(
            bookingId = BookingId(booking.id),
            status = parsedStatus,
            changedAt = BookingStatusHistoryDate(changedAt),
            changedBy = changedByUser?.let { UserId(it.id) },
            reason = reason?.let { BookingStatusReason(it) }
        )
    }
}
