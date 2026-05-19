import { describe, it, expect, vi, beforeEach } from 'vitest'
import { BookingService } from '@/domain/service/BookingService'
import type { BookingRepository } from '@/domain/port/secondary/BookingRepository'
import type { Booking, NewBooking } from '@/domain/model/Booking'

const mockBooking: Booking = {
  id: 'booking-1',
  roomId: 'room-1',
  userId: 'user-1',
  startDate: '2024-06-01',
  endDate: '2024-06-03',
  numberOfPeople: 5,
  specialRequests: null,
  status: 'PENDING',
  statusInfo: { status: 'PENDING', since: '2024-05-01T00:00:00Z', changedBy: null, reason: null },
}

const newBooking: NewBooking = {
  roomId: 'room-1',
  userId: 'user-1',
  startDate: '2024-06-01',
  endDate: '2024-06-03',
  numberOfPeople: 5,
}

describe('BookingService', () => {
  let repository: BookingRepository
  let service: BookingService

  beforeEach(() => {
    repository = {
      findAll: vi.fn().mockResolvedValue([mockBooking]),
      findByUser: vi.fn().mockResolvedValue([mockBooking]),
      create: vi.fn().mockResolvedValue(mockBooking),
      cancel: vi.fn().mockResolvedValue(undefined),
    }
    service = new BookingService(repository)
  })

  it('delegates findAll to the repository', async () => {
    const bookings = await service.findAll()
    expect(repository.findAll).toHaveBeenCalledOnce()
    expect(bookings).toEqual([mockBooking])
  })

  it('delegates findByUser with the correct userId', async () => {
    const bookings = await service.findByUser('user-1')
    expect(repository.findByUser).toHaveBeenCalledWith('user-1')
    expect(bookings).toEqual([mockBooking])
  })

  it('delegates create with the new booking data', async () => {
    const booking = await service.create(newBooking)
    expect(repository.create).toHaveBeenCalledWith(newBooking)
    expect(booking).toEqual(mockBooking)
  })

  it('delegates cancel with id and userId', async () => {
    await service.cancel('booking-1', 'user-1', 'No longer needed')
    expect(repository.cancel).toHaveBeenCalledWith('booking-1', 'user-1', 'No longer needed')
  })
})
