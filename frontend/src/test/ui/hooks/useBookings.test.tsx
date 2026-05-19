import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { useUserBookings, useCreateBooking, useCancelBooking } from '@/ui/hooks/useBookings'
import type { BookingDto } from '@/infrastructure/api/dto/BookingDto'
import type { ReactNode } from 'react'

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
  http.get(`${BASE}/users/user-1/bookings`, () => HttpResponse.json([bookingDto])),
  http.post(`${BASE}/bookings`, () => HttpResponse.json(bookingDto, { status: 201 })),
  http.delete(`${BASE}/bookings/booking-1/cancel`, () => new HttpResponse(null, { status: 204 })),
)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
}

describe('useUserBookings', () => {
  it('fetches bookings for a user and maps them to camelCase', async () => {
    const { result } = renderHook(() => useUserBookings('user-1'), { wrapper: createWrapper() })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toHaveLength(1)
    expect(result.current.data![0]).toMatchObject({
      id: 'booking-1',
      roomId: 'room-1',
      userId: 'user-1',
    })
  })

  it('is disabled when userId is empty', () => {
    const { result } = renderHook(() => useUserBookings(''), { wrapper: createWrapper() })
    expect(result.current.fetchStatus).toBe('idle')
  })

  it('exposes an error state when the API call fails', async () => {
    server.use(
      http.get(`${BASE}/users/user-1/bookings`, () => HttpResponse.json('Error', { status: 500 })),
    )
    const { result } = renderHook(() => useUserBookings('user-1'), { wrapper: createWrapper() })
    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})

describe('useCreateBooking', () => {
  it('creates a booking and returns the mapped domain model', async () => {
    const { result } = renderHook(() => useCreateBooking(), { wrapper: createWrapper() })
    result.current.mutate({
      roomId: 'room-1',
      userId: 'user-1',
      startDate: '2024-06-01',
      endDate: '2024-06-03',
      numberOfPeople: 5,
    })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data?.id).toBe('booking-1')
    expect(result.current.data?.roomId).toBe('room-1')
  })
})

describe('useCancelBooking', () => {
  it('cancels a booking successfully', async () => {
    const { result } = renderHook(() => useCancelBooking(), { wrapper: createWrapper() })
    result.current.mutate({ id: 'booking-1', userId: 'user-1' })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
  })
})
