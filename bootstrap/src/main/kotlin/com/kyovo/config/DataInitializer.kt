package com.kyovo.config

import com.kyovo.domain.model.booking.*
import com.kyovo.domain.model.room.NewRoom
import com.kyovo.domain.model.room.RoomCapacity
import com.kyovo.domain.model.room.RoomName
import com.kyovo.domain.model.user.*
import com.kyovo.domain.port.primary.AuthUseCase
import com.kyovo.domain.port.primary.BookingUseCase
import com.kyovo.domain.port.primary.RoomUseCase
import com.kyovo.infrastructure.persistence.repository.BookingJpaRepository
import com.kyovo.infrastructure.persistence.repository.BookingStatusHistoryJpaRepository
import com.kyovo.infrastructure.persistence.repository.RoomJpaRepository
import com.kyovo.infrastructure.persistence.repository.UserJpaRepository
import com.kyovo.infrastructure.persistence.repository.UserStatusHistoryJpaRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
@Profile("dev")
class DataInitializer(
    private val roomUseCase: RoomUseCase,
    private val authUseCase: AuthUseCase,
    private val bookingUseCase: BookingUseCase,
    private val bookingStatusHistoryJpaRepository: BookingStatusHistoryJpaRepository,
    private val bookingJpaRepository: BookingJpaRepository,
    private val userStatusHistoryJpaRepository: UserStatusHistoryJpaRepository,
    private val roomJpaRepository: RoomJpaRepository,
    private val userJpaRepository: UserJpaRepository
) : ApplicationRunner
{
    override fun run(args: ApplicationArguments)
    {
        bookingStatusHistoryJpaRepository.deleteAllInBatch()
        bookingJpaRepository.deleteAllInBatch()
        userStatusHistoryJpaRepository.deleteAllInBatch()
        roomJpaRepository.deleteAllInBatch()
        userJpaRepository.deleteAllInBatch()

        val admin = authUseCase.register(
            NewUser(
                UserName("Admin"),
                UserEmail("admin@example.com"),
                UserPassword("admin123"),
                UserRole.ADMIN
            )
        )

        val boardRoom = roomUseCase.save(NewRoom(RoomName("Board Room"), RoomCapacity(20), false, admin.id))
        val trainingRoom = roomUseCase.save(NewRoom(RoomName("Training Room"), RoomCapacity(30), false, admin.id))
        roomUseCase.save(NewRoom(RoomName("Focus Room"), RoomCapacity(4), false, admin.id))

        val alice = authUseCase.register(
            NewUser(
                UserName("Alice Johnson"),
                UserEmail("alice@example.com"),
                UserPassword("alice123")
            )
        )
        val bob = authUseCase.register(
            NewUser(
                UserName("Bob Smith"),
                UserEmail("bob@example.com"),
                UserPassword("bob123")
            )
        )

        bookingUseCase.create(
            NewBooking(
                roomId = boardRoom.id,
                userId = alice.id,
                startDate = BookingStartDate(LocalDate.of(2026, 6, 1)),
                endDate = BookingEndDate(LocalDate.of(2026, 6, 3)),
                numberOfPeople = BookingNumberOfPeople(8),
                specialRequests = BookingSpecialRequests("Whiteboard and projector needed")
            )
        )

        bookingUseCase.create(
            NewBooking(
                roomId = trainingRoom.id,
                userId = bob.id,
                startDate = BookingStartDate(LocalDate.of(2026, 6, 5)),
                endDate = BookingEndDate(LocalDate.of(2026, 6, 7)),
                numberOfPeople = BookingNumberOfPeople(20),
                specialRequests = null
            )
        )
    }
}
