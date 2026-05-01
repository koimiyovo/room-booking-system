package com.kyovo.infrastructure.api.security

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class TokenBlacklistService
{
    private val blacklistedTokens = ConcurrentHashMap<String, Long>()

    fun revokeToken(token: AuthToken, expirationTime: Long)
    {
        blacklistedTokens[token.value] = expirationTime
    }

    fun isTokenBlacklisted(token: AuthToken): Boolean
    {
        if (!blacklistedTokens.containsKey(token.value))
        {
            return false
        }

        val expirationTime = blacklistedTokens[token.value] ?: return false
        val isExpired = System.currentTimeMillis() > expirationTime

        if (isExpired)
        {
            blacklistedTokens.remove(token.value)
        }

        return !isExpired
    }

    @Scheduled(cron = "0 0 0 * * *")
    fun clearExpiredTokens()
    {
        val currentTime = System.currentTimeMillis()
        blacklistedTokens.entries.removeAll { (_, expirationTime) ->
            currentTime > expirationTime
        }
    }
}
