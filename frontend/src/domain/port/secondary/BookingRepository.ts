import type { Booking, NewBooking } from '@/domain/model/Booking'

export interface BookingRepository {
  findAll(): Promise<Booking[]>
  findByUser(userId: string): Promise<Booking[]>
  create(booking: NewBooking): Promise<Booking>
  cancel(id: string, userId: string, reason?: string): Promise<void>
}
