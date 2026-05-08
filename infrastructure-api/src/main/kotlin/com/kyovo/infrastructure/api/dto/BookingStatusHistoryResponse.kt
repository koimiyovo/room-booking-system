package com.kyovo.infrastructure.api.dto

import com.kyovo.domain.model.booking.BookingStatusHistory
import java.time.OffsetDateTime
import java.util.*

data class BookingStatusHistoryResponse(
    val status: String,
    val changedAt: OffsetDateTime,
    val changedBy: UUID?,
    val reason: String?
)
{
    companion object
    {
        fun fromDomain(history: BookingStatusHistory): BookingStatusHistoryResponse
        {
            return BookingStatusHistoryResponse(
                status = history.status.label,
                changedAt = history.changedAt.value,
                changedBy = history.changedBy?.value,
                reason = history.reason?.value
            )
        }
    }
}
