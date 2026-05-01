package com.kyovo.config

import com.kyovo.domain.port.secondary.*
import com.kyovo.domain.provider.TimeProvider
import com.kyovo.domain.service.AuthService
import com.kyovo.domain.service.BookingService
import com.kyovo.domain.service.RoomService
import com.kyovo.domain.service.UserService
import com.kyovo.provider.SystemTimeProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class AppConfig
{
    @Bean
    fun clock(): Clock
    {
        return Clock.systemDefaultZone()
    }

    @Bean
    fun timeProvider(clock: Clock): TimeProvider
    {
        return SystemTimeProvider(clock)
    }

    @Bean
    fun roomUseCase(roomRepository: RoomRepository): RoomService
    {
        return RoomService(roomRepository)
    }

    @Bean
    fun authUseCase(
        userRepository: UserRepository,
        passwordHashPort: PasswordHashPort,
        timeProvider: TimeProvider
    ): AuthService
    {
        return AuthService(userRepository, passwordHashPort, timeProvider)
    }

    @Bean
    fun userUseCase(userRepository: UserRepository, passwordHashPort: PasswordHashPort): UserService
    {
        return UserService(userRepository, passwordHashPort)
    }

    @Bean
    fun bookingUseCase(
        bookingRepository: BookingRepository,
        roomRepository: RoomRepository,
        transactionPort: TransactionPort
    ): BookingService
    {
        return BookingService(bookingRepository, roomRepository, transactionPort)
    }
}
