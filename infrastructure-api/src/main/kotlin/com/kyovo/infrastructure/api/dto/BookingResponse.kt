package com.kyovo.infrastructure.api.dto

import com.kyovo.domain.model.booking.Booking
import java.time.LocalDate
import java.util.*

data class BookingResponse(
    val id: UUID,
    val roomId: UUID,
    val userId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val numberOfPeople: Int,
    val specialRequests: String?,
    val status: String,
    val cancellation: CancellationResponse?
)
{
    companion object
    {
        fun fromDomain(booking: Booking): BookingResponse
        {
            return BookingResponse(
                id = booking.id.value,
                roomId = booking.roomId.value,
                userId = booking.userId.value,
                startDate = booking.startDate.value,
                endDate = booking.endDate.value,
                numberOfPeople = booking.numberOfPeople.value,
                specialRequests = booking.specialRequests?.value,
                status = booking.status.label,
                cancellation = booking.cancellation?.let {
                    CancellationResponse(cancelledBy = it.cancelledBy.value, reason = it.reason?.value)
                }
            )
        }
    }
}
