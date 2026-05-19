import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { RoomApiAdapter } from '@/infrastructure/api/RoomApiAdapter'
import type { RoomDto } from '@/infrastructure/api/dto/RoomDto'

const BASE = 'http://localhost/api/v1'

const roomDto: RoomDto = {
  id: 'room-1',
  name: 'Salle A',
  capacity: 10,
  requires_validation: true,
  created_by: 'admin-1',
}

const server = setupServer(
  http.get(`${BASE}/rooms`, () => HttpResponse.json([roomDto])),
  http.get(`${BASE}/rooms/room-1`, () => HttpResponse.json(roomDto)),
)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('RoomApiAdapter', () => {
  const adapter = new RoomApiAdapter()

  it('maps snake_case DTO fields to camelCase domain model on findAll', async () => {
    const rooms = await adapter.findAll()
    expect(rooms).toHaveLength(1)
    expect(rooms[0]).toEqual({
      id: 'room-1',
      name: 'Salle A',
      capacity: 10,
      requiresValidation: true,
      createdBy: 'admin-1',
    })
  })

  it('maps snake_case DTO fields to camelCase domain model on findById', async () => {
    const room = await adapter.findById('room-1')
    expect(room.requiresValidation).toBe(true)
    expect(room.createdBy).toBe('admin-1')
  })

  it('throws ApiError when the server returns an error', async () => {
    server.use(http.get(`${BASE}/rooms`, () => HttpResponse.json('Unauthorized', { status: 401 })))
    await expect(adapter.findAll()).rejects.toThrow()
  })
})
