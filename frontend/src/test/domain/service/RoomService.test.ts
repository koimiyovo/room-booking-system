import { describe, it, expect, vi, beforeEach } from 'vitest'
import { RoomService } from '@/domain/service/RoomService'
import type { RoomRepository } from '@/domain/port/secondary/RoomRepository'
import type { Room } from '@/domain/model/Room'

const mockRoom: Room = {
  id: 'room-1',
  name: 'Salle A',
  capacity: 10,
  requiresValidation: false,
  createdBy: 'admin-1',
}

describe('RoomService', () => {
  let repository: RoomRepository
  let service: RoomService

  beforeEach(() => {
    repository = {
      findAll: vi.fn().mockResolvedValue([mockRoom]),
      findById: vi.fn().mockResolvedValue(mockRoom),
    }
    service = new RoomService(repository)
  })

  it('delegates findAll to the repository', async () => {
    const rooms = await service.findAll()
    expect(repository.findAll).toHaveBeenCalledOnce()
    expect(rooms).toEqual([mockRoom])
  })

  it('delegates findById to the repository with the correct id', async () => {
    const room = await service.findById('room-1')
    expect(repository.findById).toHaveBeenCalledWith('room-1')
    expect(room).toEqual(mockRoom)
  })
})
