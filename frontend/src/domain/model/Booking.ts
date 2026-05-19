export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELED'

export type BookingStatusInfo = {
    status: BookingStatus
    since: string
    changedBy: string | null
    reason: string | null
}

export type Booking = {
    id: string
    roomId: string
    userId: string
    startDate: string
    endDate: string
    numberOfPeople: number
    specialRequests: string | null
    status: BookingStatus
    statusInfo: BookingStatusInfo
}

export type NewBooking = {
    roomId: string
    userId: string
    startDate: string
    endDate: string
    numberOfPeople: number
    specialRequests?: string
}
