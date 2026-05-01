package com.kyovo.domain.exception

import com.kyovo.domain.model.user.UserEmail

class EmailAlreadyUsedException(email: UserEmail) : RuntimeException("Email already in use: ${email.value}")
