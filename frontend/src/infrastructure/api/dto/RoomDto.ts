export interface RoomDto {
  id: string
  name: string
  capacity: number
  requires_validation: boolean
  created_by: string
}

export interface CreateRoomDto {
  name: string
  capacity: number
  requires_validation: boolean
}
