package com.kyovo.domain.service

import com.kyovo.domain.exception.BookingConflictException
import com.kyovo.domain.exception.RoomCapacityExceededException
import com.kyovo.domain.exception.RoomNotFoundException
import com.kyovo.domain.model.Booking
import com.kyovo.domain.model.BookingId
import com.kyovo.domain.model.NewBooking
import com.kyovo.domain.port.primary.BookingUseCase
import com.kyovo.domain.port.secondary.BookingRepository
import com.kyovo.domain.port.secondary.RoomRepository

class BookingService(
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository
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

    override fun create(newBooking: NewBooking): Booking
    {
        val room = roomRepository.findById(newBooking.roomId)
            ?: throw RoomNotFoundException(newBooking.roomId)

        if (newBooking.numberOfPeople.value > room.capacity.value)
            throw RoomCapacityExceededException(newBooking.numberOfPeople, room.capacity)

        if (bookingRepository.existsOverlappingBooking(newBooking.roomId, newBooking.startDate, newBooking.endDate))
            throw BookingConflictException(newBooking.roomId, newBooking.startDate, newBooking.endDate)

        return bookingRepository.save(newBooking.toBooking())
    }
}
