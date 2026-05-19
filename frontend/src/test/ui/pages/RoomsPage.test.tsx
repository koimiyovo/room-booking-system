import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import RoomsPage from '@/ui/pages/RoomsPage'
import type { RoomDto } from '@/infrastructure/api/dto/RoomDto'

const BASE = 'http://localhost/api/v1'

const roomDto: RoomDto = {
  id: 'room-1',
  name: 'Salle A',
  capacity: 10,
  requires_validation: false,
  created_by: 'admin-1',
}

const server = setupServer(
  http.get(`${BASE}/rooms`, () => HttpResponse.json([roomDto])),
)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

function renderRoomsPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>
        <RoomsPage />
      </QueryClientProvider>
    </MemoryRouter>,
  )
}

describe('RoomsPage', () => {
  it('shows a loading state initially', () => {
    renderRoomsPage()
    expect(screen.getByText(/loading rooms/i)).toBeDefined()
  })

  it('renders a card for each room after loading', async () => {
    renderRoomsPage()
    await waitFor(() => expect(screen.getByText('Salle A')).toBeDefined())
    expect(screen.getByText(/capacity: 10 people/i)).toBeDefined()
  })

  it('shows an empty state when no rooms are returned', async () => {
    server.use(http.get(`${BASE}/rooms`, () => HttpResponse.json([])))
    renderRoomsPage()
    await waitFor(() => expect(screen.getByText(/no rooms available/i)).toBeDefined())
  })

  it('shows an error state when the API call fails', async () => {
    server.use(http.get(`${BASE}/rooms`, () => HttpResponse.json('Error', { status: 500 })))
    renderRoomsPage()
    await waitFor(() => expect(screen.getByText(/failed to load rooms/i)).toBeDefined())
  })
})
