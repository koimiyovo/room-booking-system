export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELED'

export interface BookingStatusInfo {
  status: BookingStatus
  since: string
  changedBy: string | null
  reason: string | null
}

export interface Booking {
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

export interface NewBooking {
  roomId: string
  userId: string
  startDate: string
  endDate: string
  numberOfPeople: number
  specialRequests?: string
}
