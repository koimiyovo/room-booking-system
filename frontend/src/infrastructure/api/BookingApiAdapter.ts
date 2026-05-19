import { http } from '@/infrastructure/api/http'
import type { BookingDto, CreateBookingDto } from '@/infrastructure/api/dto/BookingDto'
import type { Booking, NewBooking } from '@/domain/model/Booking'
import type { BookingRepository } from '@/domain/port/secondary/BookingRepository'

function toBooking(dto: BookingDto): Booking {
  return {
    id: dto.id,
    roomId: dto.room_id,
    userId: dto.user_id,
    startDate: dto.start_date,
    endDate: dto.end_date,
    numberOfPeople: dto.number_of_people,
    specialRequests: dto.special_requests,
    status: dto.status,
    statusInfo: {
      status: dto.status_info.status,
      since: dto.status_info.since,
      changedBy: dto.status_info.changed_by,
      reason: dto.status_info.reason,
    },
  }
}

export class BookingApiAdapter implements BookingRepository {
  async findAll(): Promise<Booking[]> {
    const dtos = await http.get<BookingDto[]>('/bookings')
    return dtos.map(toBooking)
  }

  async findByUser(userId: string): Promise<Booking[]> {
    const dtos = await http.get<BookingDto[]>(`/users/${userId}/bookings`)
    return dtos.map(toBooking)
  }

  async create(booking: NewBooking): Promise<Booking> {
    const body: CreateBookingDto = {
      room_id: booking.roomId,
      user_id: booking.userId,
      start_date: booking.startDate,
      end_date: booking.endDate,
      number_of_people: booking.numberOfPeople,
      special_requests: booking.specialRequests,
    }
    const dto = await http.post<BookingDto>('/bookings', body)
    return toBooking(dto)
  }

  async cancel(id: string, _userId: string, reason?: string): Promise<void> {
    await http.delete(`/bookings/${id}/cancel`, reason ? { reason } : undefined)
  }
}
