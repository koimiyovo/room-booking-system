package com.kyovo.infrastructure.api.dto

import com.kyovo.domain.model.booking.BookingStatusInfo
import java.time.OffsetDateTime
import java.util.*

data class BookingStatusInfoResponse(
    val status: BookingStatusResponse,
    val since: OffsetDateTime,
    val changedBy: UUID?,
    val reason: String?
)
{
    companion object
    {
        fun fromDomain(statusInfo: BookingStatusInfo): BookingStatusInfoResponse
        {
            return BookingStatusInfoResponse(
                status = BookingStatusResponse.from(statusInfo.status),
                since = statusInfo.since.value,
                changedBy = statusInfo.changedBy?.value,
                reason = statusInfo.reason?.value
            )
        }
    }
}
