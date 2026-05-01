package com.kyovo.config

import com.kyovo.domain.port.secondary.*
import com.kyovo.domain.service.AuthService
import com.kyovo.domain.service.BookingService
import com.kyovo.domain.service.RoomService
import com.kyovo.domain.service.UserService
import com.kyovo.infrastructure.provider.SystemTimeProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfig
{
    @Bean
    fun clockPort(): ClockPort
    {
        return SystemTimeProvider()
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
        clockPort: ClockPort
    ): AuthService
    {
        return AuthService(userRepository, passwordHashPort, clockPort)
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
