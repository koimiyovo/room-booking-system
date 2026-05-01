package com.kyovo.domain.service

import com.kyovo.domain.exception.*
import com.kyovo.domain.model.booking.*
import com.kyovo.domain.model.room.Room
import com.kyovo.domain.model.room.RoomCapacity
import com.kyovo.domain.model.room.RoomId
import com.kyovo.domain.model.room.RoomName
import com.kyovo.domain.model.user.UserId
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
        override fun <T> executeInTransaction(block: () -> T): T = block()
    }
    private val bookingService = BookingService(bookingRepository, roomRepository, transactionPort)

    private val roomId = RoomId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val room = Room(roomId, RoomName("Conference Room"), RoomCapacity(10))
    private val startDate = BookingStartDate(LocalDate.of(2026, 6, 1))
    private val endDate = BookingEndDate(LocalDate.of(2026, 6, 3))
    private val newBooking = NewBooking(roomId, userId, startDate, endDate, BookingNumberOfPeople(5), null)

    private fun confirmedBooking(id: BookingId = BookingId(UUID.randomUUID())): Booking =
        Booking(id, roomId, userId, startDate, endDate, BookingNumberOfPeople(5), null, null)

    @Test
    fun `create returns booking when room exists and no conflict`()
    {
        whenever(roomRepository.findByIdForBooking(roomId)).thenReturn(room)
        whenever(bookingRepository.existsOverlappingBooking(roomId, startDate, endDate)).thenReturn(false)
        whenever(bookingRepository.save(any())).thenAnswer { it.getArgument<Booking>(0) }

        val result = bookingService.create(newBooking)

        assertThat(result.roomId).isEqualTo(roomId)
        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.status).isEqualTo(BookingStatus.CONFIRMED)
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
    fun `cancel sets status to CANCELLED stores the reason and records who cancelled`()
    {
        val bookingId = BookingId(UUID.randomUUID())
        val booking = confirmedBooking(bookingId)
        val reason = BookingCancellationReason("Change of plans")
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)
        whenever(bookingRepository.update(any())).thenAnswer { it.getArgument<Booking>(0) }

        val result = bookingService.cancel(bookingId, userId, false, reason)

        assertThat(result.status).isEqualTo(BookingStatus.CANCELLED)
        assertThat(result.cancellation?.reason).isEqualTo(reason)
        assertThat(result.cancellation?.cancelledBy).isEqualTo(userId)
    }

    @Test
    fun `cancel with isAdmin true skips ownership check and records admin as canceller`()
    {
        val bookingId = BookingId(UUID.randomUUID())
        val adminId = UserId(UUID.randomUUID())
        val otherUserId = UserId(UUID.randomUUID())
        val booking = Booking(bookingId, roomId, otherUserId, startDate, endDate, BookingNumberOfPeople(5), null, null)
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)
        whenever(bookingRepository.update(any())).thenAnswer { it.getArgument<Booking>(0) }

        val result = bookingService.cancel(bookingId, adminId, true, null)

        assertThat(result.status).isEqualTo(BookingStatus.CANCELLED)
        assertThat(result.cancellation?.cancelledBy).isEqualTo(adminId)
    }

    @Test
    fun `cancel throws BookingNotFoundException when booking does not exist`()
    {
        val bookingId = BookingId(UUID.randomUUID())
        whenever(bookingRepository.findById(bookingId)).thenReturn(null)

        assertThatThrownBy { bookingService.cancel(bookingId, userId, false, null) }
            .isInstanceOf(BookingNotFoundException::class.java)
    }

    @Test
    fun `cancel throws BookingNotOwnedByUserException when user does not own the booking`()
    {
        val bookingId = BookingId(UUID.randomUUID())
        val otherUserId = UserId(UUID.randomUUID())
        val booking = Booking(bookingId, roomId, otherUserId, startDate, endDate, BookingNumberOfPeople(5), null, null)
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)

        assertThatThrownBy { bookingService.cancel(bookingId, userId, false, null) }
            .isInstanceOf(BookingNotOwnedByUserException::class.java)
    }

    @Test
    fun `cancel throws BookingAlreadyCancelledException when booking is already cancelled`()
    {
        val bookingId = BookingId(UUID.randomUUID())
        val cancelled = Booking(
            bookingId,
            roomId,
            userId,
            startDate,
            endDate,
            BookingNumberOfPeople(5),
            null,
            Cancellation(userId, null)
        )
        whenever(bookingRepository.findById(bookingId)).thenReturn(cancelled)

        assertThatThrownBy { bookingService.cancel(bookingId, userId, false, null) }
            .isInstanceOf(BookingAlreadyCancelledException::class.java)
    }

    @Test
    fun `findByUserId returns bookings belonging to the user`()
    {
        val booking = confirmedBooking()
        whenever(bookingRepository.findByUserId(userId)).thenReturn(listOf(booking))

        val result = bookingService.findByUserId(userId)

        assertThat(result).containsExactly(booking)
    }

    @Test
    fun `findAll returns all bookings`()
    {
        val booking = confirmedBooking()
        whenever(bookingRepository.findAll()).thenReturn(listOf(booking))

        val result = bookingService.findAll()

        assertThat(result).containsExactly(booking)
    }

    @Test
    fun `findById returns booking when it exists`()
    {
        val bookingId = BookingId(UUID.randomUUID())
        val booking = confirmedBooking(bookingId)
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
