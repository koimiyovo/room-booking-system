import type { Room } from '@/domain/model/Room'

export interface RoomUseCase {
  findAll(): Promise<Room[]>
  findById(id: string): Promise<Room>
}
