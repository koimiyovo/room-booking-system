package com.kyovo.config

import com.kyovo.domain.port.secondary.BookingRepository
import com.kyovo.domain.port.secondary.PasswordHashPort
import com.kyovo.domain.port.secondary.RoomRepository
import com.kyovo.domain.port.secondary.TransactionPort
import com.kyovo.domain.port.secondary.UserRepository
import com.kyovo.domain.service.BookingService
import com.kyovo.domain.service.RoomService
import com.kyovo.domain.service.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfig
{
    @Bean
    fun roomUseCase(roomRepository: RoomRepository): RoomService
    {
        return RoomService(roomRepository)
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
