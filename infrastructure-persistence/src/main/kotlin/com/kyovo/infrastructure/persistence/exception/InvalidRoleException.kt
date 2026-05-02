package com.kyovo.infrastructure.persistence.exception

class InvalidRoleException(value: String) : RuntimeException("Invalid persisted role : $value")
