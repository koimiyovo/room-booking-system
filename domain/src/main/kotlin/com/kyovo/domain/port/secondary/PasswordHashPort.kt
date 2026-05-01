package com.kyovo.domain.port.secondary

import com.kyovo.domain.model.UserPassword

interface PasswordHashPort
{
    fun hash(raw: String): UserPassword

    fun matches(raw: String, hashed: UserPassword): Boolean
}
