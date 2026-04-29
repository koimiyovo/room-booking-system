package com.kyovo.adapter.web.security

import com.kyovo.domain.model.UserPassword
import com.kyovo.domain.port.secondary.PasswordHashPort
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptPasswordHashAdapter : PasswordHashPort
{
    private val encoder = BCryptPasswordEncoder()

    override fun hash(raw: String): UserPassword
    {
        return UserPassword(encoder.encode(raw))
    }

    override fun matches(raw: String, hashed: UserPassword): Boolean
    {
        return encoder.matches(raw, hashed.value)
    }
}
