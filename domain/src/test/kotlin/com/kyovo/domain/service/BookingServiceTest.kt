package com.kyovo.domain.service

import com.kyovo.domain.exception.*
import com.kyovo.domain.model.booking.*
import com.kyovo.domain.model.room.Room
import com.kyovo.domain.model.room.RoomCapacity
import com.kyovo.domain.model.room.RoomId
import com.kyovo.domain.model.room.RoomName
import com.kyovo.domain.model.user.*
import com.kyovo.domain.port.secondary.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

class BookingServiceTest
{
    private val bookingRepository: BookingRepository = mock()
    private val roomRepository: RoomRepository = mock()
    private val userRepository: UserRepository = mock()
    private val clockPort: ClockPort = mock()
    private val transactionPort = object : TransactionPort
    {
        override fun <T> executeInTransaction(block: () -> T): T = block()
    }
    private val bookingService =
        BookingService(bookingRepository, roomRepository, transactionPort, userRepository, clockPort)

    private val now = OffsetDateTime.now()

    private val roomId = RoomId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val creatorId = UserId(UUID.fromString("aa000000-0000-0000-0000-000000000001"))
    private val room = Room(roomId, RoomName("Conference Room"), RoomCapacity(10), requiresValidation = false, createdBy = creatorId)
    private val roomWithValidation = Room(roomId, RoomName("Board Room"), RoomCapacity(10), requiresValidation = true, createdBy = creatorId)
    private val startDate = BookingStartDate(LocalDate.of(2026, 6, 1))
    private val endDate = BookingEndDate(LocalDate.of(2026, 6, 3))
    private val newBooking = NewBooking(roomId, userId, startDate, endDate, BookingNumberOfPeople(5), null)

    private val activeUser = User(
        userId,
        UserName("Alice"),
        UserEmail("alice@example.com"),
        UserPassword("hashed"),
        UserRole.USER,
        UserRegistrationDate(OffsetDateTime.now()),
        UserStatusInfo(status = UserStatus.ACTIVE, since = UserStatusInfoDate(OffsetDateTime.now()), reason = null)
    )
    private val inactiveUser = activeUser.copy(
        statusInfo = UserStatusInfo(
            status = UserStatus.INACTIVE,
            since = UserStatusInfoDate(OffsetDateTime.now()),
            reason = null
        )
    )

    private fun confirmedBooking(id: BookingId = BookingId(UUID.randomUUID())): Booking =
        Booking(
            id, roomId, userId, startDate, endDate, BookingNumberOfPeople(5), null,
            BookingStatusInfo(BookingStatus.CONFIRMED, BookingStatusInfoDate(now), changedBy = null, reason = null)
        )

    private fun pendingBooking(id: BookingId = BookingId(UUID.randomUUID())): Booking =
        Booking(
            id, roomId, userId, startDate, endDate, BookingNumberOfPeople(5), null,
            BookingStatusInfo(BookingStatus.PENDING, BookingStatusInfoDate(now), changedBy = null, reason = null)
        )

    private fun cancelledBooking(id: BookingId = BookingId(UUID.randomUUID()), cancelledBy: UserId = userId): Booking =
        Booking(
            id, roomId, userId, startDate, endDate, BookingNumberOfPeople(5), null,
            BookingStatusInfo(
                BookingStatus.CANCELED,
                BookingStatusInfoDate(now),
                changedBy = cancelledBy,
                reason = null
            )
        )

    @Test
    fun `create returns booking when room exists and no conflict`()
    {
        whenever(clockPort.now()).thenReturn(now)
        whenever(userRepository.findById(userId)).thenReturn(activeUser)
        whenever(roomRepository.findByIdForBooking(roomId)).thenReturn(room)
        whenever(bookingRepository.existsOverlappingBooking(roomId, startDate, endDate)).thenReturn(false)
        whenever(bookingRepository.save(any())).thenAnswer { it.getArgument<Booking>(0) }

        val result = bookingService.create(newBooking)

        assertThat(result.roomId).isEqualTo(roomId)
        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.statusInfo.status).isEqualTo(BookingStatus.CONFIRMED)
    }

    @Test
    fun `create throws AccountInactiveException when user account is inactive`()
    {
        whenever(userRepository.findById(userId)).thenReturn(inactiveUser)

        assertThatThrownBy { bookingService.create(newBooking) }
            .isInstanceOf(AccountInactiveException::class.java)
    }

    @Test
    fun `create succeeds when user account is in CREATED status`()
    {
        whenever(clockPort.now()).thenReturn(now)
        val createdUser = activeUser.copy(
            statusInfo = UserStatusInfo(
                status = UserStatus.CREATED,
                since = UserStatusInfoDate(OffsetDateTime.now()),
                reason = null
            )
        )
        whenever(userRepository.findById(userId)).thenReturn(createdUser)
        whenever(roomRepository.findByIdForBooking(roomId)).thenReturn(room)
        whenever(bookingRepository.existsOverlappingBooking(roomId, startDate, endDate)).thenReturn(false)
        whenever(bookingRepository.save(any())).thenAnswer { it.getArgument<Booking>(0) }

        val result = bookingService.create(newBooking)

        assertThat(result.statusInfo.status).isEqualTo(BookingStatus.CONFIRMED)
    }

    @Test
    fun `create throws UserNotFoundException when user does not exist`()
    {
        whenever(userRepository.findById(userId)).thenReturn(null)

        assertThatThrownBy { bookingService.create(newBooking) }
            .isInstanceOf(UserNotFoundException::class.java)
    }

    @Test
    fun `create throws RoomNotFoundException when room does not exist`()
    {
        whenever(userRepository.findById(userId)).thenReturn(activeUser)
        whenever(roomRepository.findByIdForBooking(roomId)).thenReturn(null)

        assertThatThrownBy { bookingService.create(newBooking) }
            .isInstanceOf(RoomNotFoundException::class.java)
    }

    @Test
    fun `create throws RoomCapacityExceededException when number of people exceeds room capacity`()
    {
        val oversizedBooking = newBooking.copy(numberOfPeople = BookingNumberOfPeople(15))
        whenever(userRepository.findById(userId)).thenReturn(activeUser)
        whenever(roomRepository.findByIdForBooking(roomId)).thenReturn(room)

        assertThatThrownBy { bookingService.create(oversizedBooking) }
            .isInstanceOf(RoomCapacityExceededException::class.java)
    }

    @Test
    fun `create throws BookingConflictException when room is already booked for the period`()
    {
        whenever(userRepository.findById(userId)).thenReturn(activeUser)
        whenever(roomRepository.findByIdForBooking(roomId)).thenReturn(room)
        whenever(bookingRepository.existsOverlappingBooking(roomId, startDate, endDate)).thenReturn(true)

        assertThatThrownBy { bookingService.create(newBooking) }
            .isInstanceOf(BookingConflictException::class.java)
    }

    @Test
    fun `cancel sets status to CANCELLED stores the reason and records who cancelled`()
    {
        whenever(clockPort.now()).thenReturn(now)
        val bookingId = BookingId(UUID.randomUUID())
        val booking = confirmedBooking(bookingId)
        val reason = BookingStatusReason("Change of plans")
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)
        whenever(bookingRepository.update(any())).thenAnswer { it.getArgument<Booking>(0) }

        val result = bookingService.cancel(bookingId, userId, false, reason)

        assertThat(result.statusInfo.status).isEqualTo(BookingStatus.CANCELED)
        assertThat(result.statusInfo.reason).isEqualTo(reason)
        assertThat(result.statusInfo.changedBy).isEqualTo(userId)
    }

    @Test
    fun `cancel with isAdmin true skips ownership check and records admin as canceller`()
    {
        whenever(clockPort.now()).thenReturn(now)
        val bookingId = BookingId(UUID.randomUUID())
        val adminId = UserId(UUID.randomUUID())
        val otherUserId = UserId(UUID.randomUUID())
        val booking = confirmedBooking(bookingId).copy(userId = otherUserId)
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)
        whenever(bookingRepository.update(any())).thenAnswer { it.getArgument<Booking>(0) }

        val result = bookingService.cancel(bookingId, adminId, true, null)

        assertThat(result.statusInfo.status).isEqualTo(BookingStatus.CANCELED)
        assertThat(result.statusInfo.changedBy).isEqualTo(adminId)
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
        val booking = confirmedBooking(bookingId).copy(userId = otherUserId)
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)

        assertThatThrownBy { bookingService.cancel(bookingId, userId, false, null) }
            .isInstanceOf(BookingNotOwnedByUserException::class.java)
    }

    @Test
    fun `cancel throws BookingAlreadyCancelledException when booking is already cancelled`()
    {
        whenever(clockPort.now()).thenReturn(now)
        val bookingId = BookingId(UUID.randomUUID())
        val booking = cancelledBooking(bookingId)
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)

        assertThatThrownBy { bookingService.cancel(bookingId, userId, false, null) }
            .isInstanceOf(BookingAlreadyCancelledException::class.java)
    }

    @Test
    fun `cancel records the timestamp of cancellation in statusInfo`()
    {
        whenever(clockPort.now()).thenReturn(now)
        val bookingId = BookingId(UUID.randomUUID())
        val booking = confirmedBooking(bookingId)
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)
        whenever(bookingRepository.update(any())).thenAnswer { it.getArgument<Booking>(0) }

        val result = bookingService.cancel(bookingId, userId, false, null)

        assertThat(result.statusInfo.since.value).isEqualTo(now)
    }

    @Test
    fun `create records CONFIRMED status in statusInfo`()
    {
        whenever(clockPort.now()).thenReturn(now)
        whenever(userRepository.findById(userId)).thenReturn(activeUser)
        whenever(roomRepository.findByIdForBooking(roomId)).thenReturn(room)
        whenever(bookingRepository.existsOverlappingBooking(roomId, startDate, endDate)).thenReturn(false)
        whenever(bookingRepository.save(any())).thenAnswer { it.getArgument<Booking>(0) }

        val result = bookingService.create(newBooking)

        assertThat(result.statusInfo.status).isEqualTo(BookingStatus.CONFIRMED)
        assertThat(result.statusInfo.changedBy).isNull()
        assertThat(result.statusInfo.since.value).isEqualTo(now)
    }

    @Test
    fun `findStatusHistory throws BookingNotFoundException when booking does not exist`()
    {
        val bookingId = BookingId(UUID.randomUUID())
        whenever(bookingRepository.findById(bookingId)).thenReturn(null)

        assertThatThrownBy { bookingService.findStatusHistory(bookingId) }
            .isInstanceOf(BookingNotFoundException::class.java)
    }

    @Test
    fun `findStatusHistory returns history when booking exists`()
    {
        val bookingId = BookingId(UUID.randomUUID())
        val booking = confirmedBooking(bookingId)
        val history = listOf(
            BookingStatusHistory(bookingId, BookingStatus.CONFIRMED, BookingStatusHistoryDate(now), userId, null)
        )
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)
        whenever(bookingRepository.findStatusHistory(bookingId)).thenReturn(history)

        val result = bookingService.findStatusHistory(bookingId)

        assertThat(result).isEqualTo(history)
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

    @Test
    fun `create returns PENDING booking when room requiresValidation is true`()
    {
        whenever(clockPort.now()).thenReturn(now)
        whenever(userRepository.findById(userId)).thenReturn(activeUser)
        whenever(roomRepository.findByIdForBooking(roomId)).thenReturn(roomWithValidation)
        whenever(bookingRepository.existsOverlappingBooking(roomId, startDate, endDate)).thenReturn(false)
        whenever(bookingRepository.save(any())).thenAnswer { it.getArgument<Booking>(0) }

        val result = bookingService.create(newBooking)

        assertThat(result.status).isEqualTo(BookingStatus.PENDING)
        assertThat(result.statusInfo.changedBy).isNull()
    }

    @Test
    fun `validate transitions PENDING to CONFIRMED and records admin id`()
    {
        whenever(clockPort.now()).thenReturn(now)
        val adminId = UserId(UUID.randomUUID())
        val bookingId = BookingId(UUID.randomUUID())
        val booking = pendingBooking(bookingId)
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)
        whenever(bookingRepository.update(any())).thenAnswer { it.getArgument<Booking>(0) }
        whenever(bookingRepository.findOverlappingPendingBookings(roomId, startDate, endDate, bookingId)).thenReturn(emptyList())

        val result = bookingService.validate(bookingId, adminId)

        assertThat(result.status).isEqualTo(BookingStatus.CONFIRMED)
        assertThat(result.statusInfo.changedBy).isEqualTo(adminId)
        assertThat(result.statusInfo.since.value).isEqualTo(now)
    }

    @Test
    fun `validate throws BookingNotFoundException when booking does not exist`()
    {
        val bookingId = BookingId(UUID.randomUUID())
        whenever(bookingRepository.findById(bookingId)).thenReturn(null)

        assertThatThrownBy { bookingService.validate(bookingId, userId) }
            .isInstanceOf(BookingNotFoundException::class.java)
    }

    @Test
    fun `validate throws BookingNotPendingException when booking is already confirmed`()
    {
        val bookingId = BookingId(UUID.randomUUID())
        val booking = confirmedBooking(bookingId)
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)

        assertThatThrownBy { bookingService.validate(bookingId, userId) }
            .isInstanceOf(BookingNotPendingException::class.java)
    }

    @Test
    fun `validate throws BookingNotPendingException when booking is cancelled`()
    {
        val bookingId = BookingId(UUID.randomUUID())
        val booking = cancelledBooking(bookingId)
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)

        assertThatThrownBy { bookingService.validate(bookingId, userId) }
            .isInstanceOf(BookingNotPendingException::class.java)
    }

    @Test
    fun `validate cancels overlapping PENDING bookings with system reason`()
    {
        whenever(clockPort.now()).thenReturn(now)
        val adminId = UserId(UUID.randomUUID())
        val bookingId = BookingId(UUID.randomUUID())
        val booking = pendingBooking(bookingId)
        val conflictingId = BookingId(UUID.randomUUID())
        val conflicting = pendingBooking(conflictingId)
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)
        whenever(bookingRepository.update(any())).thenAnswer { it.getArgument<Booking>(0) }
        whenever(bookingRepository.findOverlappingPendingBookings(roomId, startDate, endDate, bookingId))
            .thenReturn(listOf(conflicting))

        bookingService.validate(bookingId, adminId)

        org.mockito.kotlin.verify(bookingRepository).saveStatusHistory(
            org.mockito.kotlin.eq(conflictingId),
            org.mockito.kotlin.eq(BookingStatus.CANCELED),
            any(),
            org.mockito.kotlin.isNull(),
            any()
        )
    }
}
