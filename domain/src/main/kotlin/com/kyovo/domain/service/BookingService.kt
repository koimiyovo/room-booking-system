package com.kyovo.domain.service

import com.kyovo.domain.exception.*
import com.kyovo.domain.model.booking.*
import com.kyovo.domain.model.user.UserId
import com.kyovo.domain.port.primary.BookingUseCase
import com.kyovo.domain.port.secondary.BookingRepository
import com.kyovo.domain.port.secondary.RoomRepository
import com.kyovo.domain.port.secondary.TransactionPort
import com.kyovo.domain.port.secondary.UserRepository

class BookingService(
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
    private val transactionPort: TransactionPort,
    private val userRepository: UserRepository
) : BookingUseCase
{
    override fun findAll(): List<Booking>
    {
        return bookingRepository.findAll()
    }

    override fun findById(id: BookingId): Booking?
    {
        return bookingRepository.findById(id)
    }

    override fun findByUserId(userId: UserId): List<Booking>
    {
        return bookingRepository.findByUserId(userId)
    }

    override fun create(newBooking: NewBooking): Booking
    {
        return transactionPort.executeInTransaction {
            val user = userRepository.findById(newBooking.userId) ?: throw UserNotFoundException(newBooking.userId)
            if (user.isInactive()) throw AccountInactiveException()

            val room = roomRepository.findByIdForBooking(newBooking.roomId)
                ?: throw RoomNotFoundException(newBooking.roomId)

            if (newBooking.numberOfPeople.value > room.capacity.value)
                throw RoomCapacityExceededException(newBooking.numberOfPeople, room.capacity)

            if (bookingRepository.existsOverlappingBooking(newBooking.roomId, newBooking.startDate, newBooking.endDate))
                throw BookingConflictException(newBooking.roomId, newBooking.startDate, newBooking.endDate)

            bookingRepository.save(newBooking.toBooking())
        }
    }

    override fun cancel(
        bookingId: BookingId,
        cancelledBy: UserId,
        isAdmin: Boolean,
        reason: BookingCancellationReason?
    ): Booking
    {
        return transactionPort.executeInTransaction {
            val booking = bookingRepository.findById(bookingId) ?: throw BookingNotFoundException(bookingId)
            if (!isAdmin && booking.userId != cancelledBy)
                throw BookingNotOwnedByUserException(bookingId, cancelledBy)
            if (booking.cancellation != null) throw BookingAlreadyCancelledException(bookingId)
            bookingRepository.update(booking.copy(cancellation = Cancellation(cancelledBy, reason)))
        }
    }
}
