import { useQuery } from '@tanstack/react-query'
import { RoomService } from '@/domain/service/RoomService'
import { RoomApiAdapter } from '@/infrastructure/api/RoomApiAdapter'

const roomService = new RoomService(new RoomApiAdapter())

export function useRooms() {
  return useQuery({
    queryKey: ['rooms'],
    queryFn: () => roomService.findAll(),
  })
}

export function useRoom(id: string) {
  return useQuery({
    queryKey: ['rooms', id],
    queryFn: () => roomService.findById(id),
    enabled: !!id,
  })
}
