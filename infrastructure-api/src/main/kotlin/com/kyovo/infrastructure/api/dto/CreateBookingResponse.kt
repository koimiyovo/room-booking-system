package com.kyovo.infrastructure.api.dto

import java.time.LocalDate
import java.util.*

data class CreateBookingResponse(
    val id: UUID,
    val roomId: UUID,
    val userId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val numberOfPeople: Int,
    val specialRequests: String?
)
