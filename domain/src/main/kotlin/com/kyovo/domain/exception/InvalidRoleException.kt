package com.kyovo.domain.exception

class InvalidRoleException(value: String) : RuntimeException("Invalid role : $value")