import type { Room } from '@/domain/model/Room'
import type { RoomUseCase } from '@/domain/port/primary/RoomUseCase'
import type { RoomRepository } from '@/domain/port/secondary/RoomRepository'

export class RoomService implements RoomUseCase {
  constructor(private readonly repository: RoomRepository) {}

  findAll(): Promise<Room[]> {
    return this.repository.findAll()
  }

  findById(id: string): Promise<Room> {
    return this.repository.findById(id)
  }
}
