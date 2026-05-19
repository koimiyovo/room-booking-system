import type { Booking } from '@/domain/model/Booking'
import { Badge } from '@/components/ui/badge'

interface Props {
  booking: Booking
}

const statusVariant: Record<string, 'default' | 'secondary' | 'destructive'> = {
  PENDING: 'secondary',
  CONFIRMED: 'default',
  CANCELED: 'destructive',
}

export default function BookingRow({ booking }: Props) {
  return (
    <div className="border rounded-lg p-4 bg-white flex items-center justify-between">
      <div className="space-y-1">
        <p className="text-sm font-medium">
          {booking.startDate} → {booking.endDate}
        </p>
        <p className="text-xs text-gray-500">{booking.numberOfPeople} people</p>
        {booking.specialRequests && (
          <p className="text-xs text-gray-400 italic">{booking.specialRequests}</p>
        )}
      </div>
      <Badge variant={statusVariant[booking.status] ?? 'secondary'}>{booking.status}</Badge>
    </div>
  )
}
