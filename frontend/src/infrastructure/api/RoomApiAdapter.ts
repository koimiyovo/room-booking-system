import { http } from '@/infrastructure/api/http'
import type { RoomDto } from '@/infrastructure/api/dto/RoomDto'
import type { Room } from '@/domain/model/Room'
import type { RoomRepository } from '@/domain/port/secondary/RoomRepository'

function toRoom(dto: RoomDto): Room {
  return {
    id: dto.id,
    name: dto.name,
    capacity: dto.capacity,
    requiresValidation: dto.requires_validation,
    createdBy: dto.created_by,
  }
}

export class RoomApiAdapter implements RoomRepository {
  async findAll(): Promise<Room[]> {
    const dtos = await http.get<RoomDto[]>('/rooms')
    return dtos.map(toRoom)
  }

  async findById(id: string): Promise<Room> {
    const dto = await http.get<RoomDto>(`/rooms/${id}`)
    return toRoom(dto)
  }
}
