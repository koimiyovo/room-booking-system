import type { Booking, NewBooking } from '@/domain/model/Booking'
import type { BookingUseCase } from '@/domain/port/primary/BookingUseCase'
import type { BookingRepository } from '@/domain/port/secondary/BookingRepository'

export class BookingService implements BookingUseCase {
  constructor(private readonly repository: BookingRepository) {}

  findAll(): Promise<Booking[]> {
    return this.repository.findAll()
  }

  findByUser(userId: string): Promise<Booking[]> {
    return this.repository.findByUser(userId)
  }

  create(booking: NewBooking): Promise<Booking> {
    return this.repository.create(booking)
  }

  cancel(id: string, userId: string, reason?: string): Promise<void> {
    return this.repository.cancel(id, userId, reason)
  }
}
