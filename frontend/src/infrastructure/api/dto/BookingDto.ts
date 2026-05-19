export type BookingStatusDto = 'PENDING' | 'CONFIRMED' | 'CANCELED'

export type BookingStatusInfoDto = {
    status: BookingStatusDto
    since: string
    changed_by: string | null
    reason: string | null
}

export type BookingDto = {
    id: string
    room_id: string
    user_id: string
    start_date: string
    end_date: string
    number_of_people: number
    special_requests: string | null
    status: BookingStatusDto
    status_info: BookingStatusInfoDto
}

export type CreateBookingDto = {
    room_id: string
    user_id: string
    start_date: string
    end_date: string
    number_of_people: number
    special_requests?: string
}
