package com.kyovo.adapter.web.security

import com.kyovo.domain.model.UserId
import com.kyovo.domain.model.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    @param:Value($$"${app.jwt.secret}") private val secret: String,
    @param:Value($$"${app.jwt.expiration-ms}") private val expirationMs: Long
)
{
    private val key: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))

    fun generateToken(userId: UserId, role: UserRole): AuthToken
    {
        return AuthToken(
            Jwts.builder()
                .subject(userId.value.toString())
                .claim("role", role.label)
                .issuedAt(Date())
                .expiration(Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact()
        )
    }

    fun extractUserId(token: AuthToken): UserId?
    {
        return parseClaims(token)?.subject?.let { UserId(UUID.fromString(it)) }
    }

    fun extractRole(token: AuthToken): UserRole?
    {
        return parseClaims(token)?.get("role", String::class.java)?.let { UserRole.valueOf(it) }
    }

    fun validateToken(token: AuthToken): Boolean
    {
        return parseClaims(token) != null
    }

    fun extractExpirationTime(token: AuthToken): Long?
    {
        return parseClaims(token)?.expiration?.time
    }

    private fun parseClaims(token: AuthToken): Claims?
    {
        return try
        {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token.value).payload
        }
        catch (e: Exception)
        {
            null
        }
    }
}
