package com.kyovo.config

import com.kyovo.provider.MutableTimeProvider
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@TestConfiguration
class TestTimeProviderConfig
{
    @Bean
    @Primary
    fun testTimeProvider(): MutableTimeProvider
    {
        return MutableTimeProvider(OffsetDateTime.of(LocalDateTime.of(2026, 1, 1, 0, 0), ZoneOffset.UTC))
    }
}