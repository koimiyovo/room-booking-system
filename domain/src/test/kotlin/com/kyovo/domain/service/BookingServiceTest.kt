package com.kyovo.domain.service

import com.kyovo.domain.exception.BookingConflictException
import com.kyovo.domain.exception.RoomCapacityExceededException
import com.kyovo.domain.exception.RoomNotFoundException
import com.kyovo.domain.model.*
import com.kyovo.domain.port.secondary.BookingRepository
import com.kyovo.domain.port.secondary.RoomRepository
import com.kyovo.domain.port.secondary.TransactionPort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.*

class BookingServiceTest
{

    private val bookingRepository: BookingRepository = mock()
    private val roomRepository: RoomRepository = mock()
    private val transactionPort = object : TransactionPort
    {
        override fun <T> executeInTransaction(block: () -> T): T
        {
            return block()
        }
    }
    private val bookingService = BookingService(bookingRepository, roomRepository, transactionPort)

    private val roomId = RoomId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val room = Room(roomId, RoomName("Conference Room"), RoomCapacity(10))
    private val startDate = BookingStartDate(LocalDate.of(2026, 6, 1))
    private val endDate = BookingEndDate(LocalDate.of(2026, 6, 3))
    private val newBooking = NewBooking(roomId, userId, startDate, endDate, BookingNumberOfPeople(5), null)

    @Test
    fun `create returns booking when room exists and no conflict`()
    {
        whenever(roomRepository.findByIdForBooking(roomId)).thenReturn(room)
        whenever(bookingRepository.existsOverlappingBooking(roomId, startDate, endDate)).thenReturn(false)
        whenever(bookingRepository.save(any())).thenAnswer { it.getArgument<Booking>(0) }

        val result = bookingService.create(newBooking)

        assertThat(result.roomId).isEqualTo(roomId)
        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.startDate).isEqualTo(startDate)
        assertThat(result.endDate).isEqualTo(endDate)
        assertThat(result.numberOfPeople).isEqualTo(BookingNumberOfPeople(5))
        assertThat(result.specialRequests).isNull()
    }

    @Test
    fun `create throws RoomNotFoundException when room does not exist`()
    {
        whenever(roomRepository.findByIdForBooking(roomId)).thenReturn(null)

        assertThatThrownBy { bookingService.create(newBooking) }
            .isInstanceOf(RoomNotFoundException::class.java)
    }

    @Test
    fun `create throws RoomCapacityExceededException when number of people exceeds room capacity`()
    {
        val oversizedBooking = newBooking.copy(numberOfPeople = BookingNumberOfPeople(15))
        whenever(roomRepository.findByIdForBooking(roomId)).thenReturn(room)

        assertThatThrownBy { bookingService.create(oversizedBooking) }
            .isInstanceOf(RoomCapacityExceededException::class.java)
    }

    @Test
    fun `create throws BookingConflictException when room is already booked for the period`()
    {
        whenever(roomRepository.findByIdForBooking(roomId)).thenReturn(room)
        whenever(bookingRepository.existsOverlappingBooking(roomId, startDate, endDate)).thenReturn(true)

        assertThatThrownBy { bookingService.create(newBooking) }
            .isInstanceOf(BookingConflictException::class.java)
    }

    @Test
    fun `findAll returns all bookings`()
    {
        val booking =
            Booking(BookingId(UUID.randomUUID()), roomId, userId, startDate, endDate, BookingNumberOfPeople(5), null)
        whenever(bookingRepository.findAll()).thenReturn(listOf(booking))

        val result = bookingService.findAll()

        assertThat(result).containsExactly(booking)
    }

    @Test
    fun `findAll returns empty list when no bookings exist`()
    {
        whenever(bookingRepository.findAll()).thenReturn(emptyList())

        val result = bookingService.findAll()

        assertThat(result).isEmpty()
    }

    @Test
    fun `findById returns booking when it exists`()
    {
        val bookingId = BookingId(UUID.randomUUID())
        val booking = Booking(bookingId, roomId, userId, startDate, endDate, BookingNumberOfPeople(5), null)
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)

        val result = bookingService.findById(bookingId)

        assertThat(result).isEqualTo(booking)
    }

    @Test
    fun `findById returns null when booking does not exist`()
    {
        val bookingId = BookingId(UUID.randomUUID())
        whenever(bookingRepository.findById(bookingId)).thenReturn(null)

        val result = bookingService.findById(bookingId)

        assertThat(result).isNull()
    }
}
