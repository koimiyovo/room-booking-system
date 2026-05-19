import { useAuthContext } from '@/ui/context/AuthContext'
import { useBookings, useUserBookings } from '@/ui/hooks/useBookings'
import BookingRow from '@/ui/components/bookings/BookingRow'

export default function BookingsPage() {
  const { isAdmin, user } = useAuthContext()
  const allBookings = useBookings()
  const userBookings = useUserBookings(user?.id ?? '')

  const { data: bookings, isLoading, error } = isAdmin ? allBookings : userBookings

  if (isLoading) {
    return <p className="text-center py-12 text-gray-400">Loading bookings...</p>
  }
  if (error) {
    return <p className="text-center py-12 text-red-500">Failed to load bookings.</p>
  }

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Bookings</h1>
      {bookings?.length === 0 ? (
        <p className="text-gray-400">No bookings found.</p>
      ) : (
        <div className="space-y-3">
          {bookings?.map(b => <BookingRow key={b.id} booking={b} />)}
        </div>
      )}
    </div>
  )
}
