import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { useRooms } from '@/ui/hooks/useRooms'
import type { RoomDto } from '@/infrastructure/api/dto/RoomDto'
import type { ReactNode } from 'react'

const BASE = 'http://localhost/api/v1'

const roomDto: RoomDto = {
  id: 'room-1',
  name: 'Salle A',
  capacity: 8,
  requires_validation: false,
  created_by: 'admin-1',
}

const server = setupServer(
  http.get(`${BASE}/rooms`, () => HttpResponse.json([roomDto])),
)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
}

describe('useRooms', () => {
  it('fetches rooms and maps them to camelCase domain models', async () => {
    const { result } = renderHook(() => useRooms(), { wrapper: createWrapper() })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toHaveLength(1)
    expect(result.current.data![0]).toMatchObject({
      id: 'room-1',
      name: 'Salle A',
      requiresValidation: false,
      createdBy: 'admin-1',
    })
  })

  it('exposes an error state when the API call fails', async () => {
    server.use(http.get(`${BASE}/rooms`, () => HttpResponse.json('Error', { status: 500 })))
    const { result } = renderHook(() => useRooms(), { wrapper: createWrapper() })
    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})
