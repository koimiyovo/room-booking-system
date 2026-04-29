package com.kyovo.adapter.web.security

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class TokenBlacklistService
{
    private val blacklistedTokens = ConcurrentHashMap<String, Long>()

    fun revokeToken(token: String, expirationTime: Long)
    {
        blacklistedTokens[token] = expirationTime
    }

    fun isTokenBlacklisted(token: String): Boolean
    {
        if (!blacklistedTokens.containsKey(token))
        {
            return false
        }

        val expirationTime = blacklistedTokens[token] ?: return false
        val isExpired = System.currentTimeMillis() > expirationTime

        if (isExpired)
        {
            blacklistedTokens.remove(token)
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
