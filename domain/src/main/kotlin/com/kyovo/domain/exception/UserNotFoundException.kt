package com.kyovo.domain.exception

import com.kyovo.domain.model.UserId

class UserNotFoundException(id: UserId) : RuntimeException("User not found: ${id.value}")
