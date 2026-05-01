package com.kyovo.domain.exception

import com.kyovo.domain.model.user.UserId

class UserAlreadyActiveException(id: UserId) : RuntimeException("User is already active : ${id.value}")
