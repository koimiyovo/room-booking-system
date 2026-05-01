package com.kyovo.adapter.persistence.exception

class InvalidRoleException(value: String) : RuntimeException("Invalid persisted role: $value")
