import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { BookingApiAdapter } from '@/infrastructure/api/BookingApiAdapter'
import type { BookingDto } from '@/infrastructure/api/dto/BookingDto'

const BASE = 'http://localhost/api/v1'

const bookingDto: BookingDto = {
  id: 'booking-1',
  room_id: 'room-1',
  user_id: 'user-1',
  start_date: '2024-06-01',
  end_date: '2024-06-03',
  number_of_people: 5,
  special_requests: null,
  status: 'PENDING',
  status_info: {
    status: 'PENDING',
    since: '2024-05-01T00:00:00Z',
    changed_by: null,
    reason: null,
  },
}

const server = setupServer(
  http.get(`${BASE}/bookings`, () => HttpResponse.json([bookingDto])),
  http.get(`${BASE}/users/user-1/bookings`, () => HttpResponse.json([bookingDto])),
  http.post(`${BASE}/bookings`, () => HttpResponse.json(bookingDto, { status: 201 })),
  http.delete(`${BASE}/bookings/booking-1/cancel`, () => new HttpResponse(null, { status: 204 })),
)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('BookingApiAdapter', () => {
  const adapter = new BookingApiAdapter()

  it('maps snake_case DTO fields to camelCase domain model on findAll', async () => {
    const bookings = await adapter.findAll()
    expect(bookings).toHaveLength(1)
    expect(bookings[0]).toEqual({
      id: 'booking-1',
      roomId: 'room-1',
      userId: 'user-1',
      startDate: '2024-06-01',
      endDate: '2024-06-03',
      numberOfPeople: 5,
      specialRequests: null,
      status: 'PENDING',
      statusInfo: {
        status: 'PENDING',
        since: '2024-05-01T00:00:00Z',
        changedBy: null,
        reason: null,
      },
    })
  })

  it('fetches bookings for a specific user', async () => {
    const bookings = await adapter.findByUser('user-1')
    expect(bookings).toHaveLength(1)
    expect(bookings[0].userId).toBe('user-1')
  })

  it('sends a create request with snake_case body and maps the response', async () => {
    const booking = await adapter.create({
      roomId: 'room-1',
      userId: 'user-1',
      startDate: '2024-06-01',
      endDate: '2024-06-03',
      numberOfPeople: 5,
    })
    expect(booking.id).toBe('booking-1')
    expect(booking.roomId).toBe('room-1')
    expect(booking.statusInfo.changedBy).toBeNull()
  })

  it('sends a cancel request and resolves without a value', async () => {
    await expect(adapter.cancel('booking-1', 'user-1')).resolves.toBeUndefined()
  })

  it('throws when the server returns an error', async () => {
    server.use(http.get(`${BASE}/bookings`, () => HttpResponse.json('Forbidden', { status: 403 })))
    await expect(adapter.findAll()).rejects.toThrow()
  })
})
