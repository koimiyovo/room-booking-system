package com.kyovo.infrastructure.persistence.exception

class InvalidUserStatusException(value: String) : RuntimeException("Invalid persisted user status : $value")
