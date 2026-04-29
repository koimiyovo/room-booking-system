package com.kyovo.adapter.web.dto

import java.time.LocalDate
import java.util.UUID

data class CreateBookingResponse(
    val id: UUID,
    val roomId: UUID,
    val userId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val numberOfPeople: Int,
    val specialRequests: String?
)
