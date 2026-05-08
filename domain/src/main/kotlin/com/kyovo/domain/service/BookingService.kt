package com.kyovo.domain.service

import com.kyovo.domain.exception.*
import com.kyovo.domain.model.booking.*
import com.kyovo.domain.model.user.UserId
import com.kyovo.domain.port.primary.BookingUseCase
import com.kyovo.domain.port.secondary.*

class BookingService(
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
    private val transactionPort: TransactionPort,
    private val userRepository: UserRepository,
    private val clockPort: ClockPort
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

            val now = clockPort.now()
            val statusInfo =
                BookingStatusInfo(BookingStatus.CONFIRMED, BookingStatusInfoDate(now), changedBy = null, reason = null)
            val booking = bookingRepository.save(newBooking.toBooking(statusInfo))
            bookingRepository.saveStatusHistory(
                booking.id,
                BookingStatus.CONFIRMED,
                BookingStatusHistoryDate(now),
                newBooking.userId,
                null
            )
            booking
        }
    }

    override fun cancel(
        bookingId: BookingId,
        cancelledBy: UserId,
        isAdmin: Boolean,
        reason: BookingStatusReason?
    ): Booking
    {
        return transactionPort.executeInTransaction {
            val booking = bookingRepository.findById(bookingId) ?: throw BookingNotFoundException(bookingId)
            if (!isAdmin && booking.userId != cancelledBy)
                throw BookingNotOwnedByUserException(bookingId, cancelledBy)
            val now = clockPort.now()
            val updated = booking.transitionTo(BookingStatus.CANCELED, BookingStatusInfoDate(now), cancelledBy, reason)
                ?: throw BookingAlreadyCancelledException(bookingId)
            val saved = bookingRepository.update(updated)
            bookingRepository.saveStatusHistory(
                saved.id,
                BookingStatus.CANCELED,
                BookingStatusHistoryDate(now),
                cancelledBy,
                reason
            )
            saved
        }
    }

    override fun findStatusHistory(bookingId: BookingId): List<BookingStatusHistory>
    {
        bookingRepository.findById(bookingId) ?: throw BookingNotFoundException(bookingId)
        return bookingRepository.findStatusHistory(bookingId)
    }
}
