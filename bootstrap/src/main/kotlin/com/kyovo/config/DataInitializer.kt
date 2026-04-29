package com.kyovo.config

import com.kyovo.domain.model.*
import com.kyovo.domain.port.primary.BookingUseCase
import com.kyovo.domain.port.primary.RoomUseCase
import com.kyovo.domain.port.primary.UserUseCase
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DataInitializer(
    private val roomUseCase: RoomUseCase,
    private val userUseCase: UserUseCase,
    private val bookingUseCase: BookingUseCase
) : ApplicationRunner
{
    override fun run(args: ApplicationArguments)
    {
        val boardRoom = roomUseCase.save(NewRoom(RoomName("Board Room"), RoomCapacity(20)))
        val trainingRoom = roomUseCase.save(NewRoom(RoomName("Training Room"), RoomCapacity(30)))
        roomUseCase.save(NewRoom(RoomName("Focus Room"), RoomCapacity(4)))

        userUseCase.save(
            NewUser(
                UserName("Admin"),
                UserEmail("admin@example.com"),
                UserPassword("admin123"),
                UserRole.ADMIN
            )
        )
        val alice = userUseCase.save(
            NewUser(
                UserName("Alice Johnson"),
                UserEmail("alice@example.com"),
                UserPassword("alice123")
            )
        )
        val bob = userUseCase.save(NewUser(UserName("Bob Smith"), UserEmail("bob@example.com"), UserPassword("bob123")))

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
