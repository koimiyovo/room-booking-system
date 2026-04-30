package com.kyovo.adapter.web.security

import com.kyovo.domain.model.UserPassword
import com.kyovo.domain.port.secondary.PasswordHashPort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptPasswordHashAdapter(private val encoder: PasswordEncoder) : PasswordHashPort
{
    override fun hash(raw: String): UserPassword
    {
        return UserPassword(encoder.encode(raw))
    }

    override fun matches(raw: String, hashed: UserPassword): Boolean
    {
        return encoder.matches(raw, hashed.value)
    }
}
