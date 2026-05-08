package com.kyovo.domain.model.booking

enum class BookingStatus(val label: String)
{
    CONFIRMED("CONFIRMED"),
    CANCELED("CANCELLED");

    val allowedTransitions: Set<BookingStatus> by lazy {
        when (this)
        {
            CONFIRMED -> setOf(CANCELED)
            CANCELED -> emptySet()
        }
    }

    fun canTransitionTo(target: BookingStatus): Boolean
    {
        return target in allowedTransitions
    }
}
