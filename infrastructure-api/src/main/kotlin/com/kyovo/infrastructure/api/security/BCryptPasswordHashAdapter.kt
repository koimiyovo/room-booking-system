package com.kyovo.infrastructure.api.security

import com.kyovo.domain.model.user.UserPassword
import com.kyovo.domain.port.secondary.PasswordHashPort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptPasswordHashAdapter(private val encoder: PasswordEncoder) : PasswordHashPort
{
    override fun hash(raw: String): UserPassword
    {
        val encoded = encoder.encode(raw) ?: error("Cannot encode user password")
        return UserPassword(encoded)
    }

    override fun matches(raw: String, hashed: UserPassword): Boolean
    {
        return encoder.matches(raw, hashed.value)
    }
}
