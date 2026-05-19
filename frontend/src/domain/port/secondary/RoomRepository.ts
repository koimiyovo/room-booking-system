import type { Room } from '@/domain/model/Room'

export interface RoomRepository {
  findAll(): Promise<Room[]>
  findById(id: string): Promise<Room>
}
