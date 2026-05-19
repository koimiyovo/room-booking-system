export type RoomDto = {
    id: string
    name: string
    capacity: number
    requires_validation: boolean
    created_by: string
}

export type CreateRoomDto = {
    name: string
    capacity: number
    requires_validation: boolean
}
