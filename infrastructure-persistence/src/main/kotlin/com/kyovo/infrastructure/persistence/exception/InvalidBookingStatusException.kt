package com.kyovo.infrastructure.persistence.exception

class InvalidBookingStatusException(value: String) : RuntimeException("Invalid persisted booking status : $value")
