package com.kyovo.infrastructure.api.dto

import com.kyovo.infrastructure.api.security.AuthToken

data class LoginResponse(val token: AuthToken)
