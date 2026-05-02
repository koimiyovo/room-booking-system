package com.kyovo.domain.exception

import com.kyovo.domain.model.user.UserId

class UserNotFoundException(id: UserId) : RuntimeException("User not found: ${id.value}")
