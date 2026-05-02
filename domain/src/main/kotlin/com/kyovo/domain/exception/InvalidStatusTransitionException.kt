package com.kyovo.domain.exception

import com.kyovo.domain.model.user.UserStatus

class InvalidStatusTransitionException(from: UserStatus, to: UserStatus) :
    RuntimeException("Invalid status transition from ${from.label} to ${to.label}")
