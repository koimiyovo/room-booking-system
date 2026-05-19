import { useRooms } from '@/ui/hooks/useRooms'
import RoomCard from '@/ui/components/rooms/RoomCard'

export default function RoomsPage() {
  const { data: rooms, isLoading, error } = useRooms()

  if (isLoading) {
    return <p className="text-center py-12 text-gray-400">Loading rooms...</p>
  }
  if (error) {
    return <p className="text-center py-12 text-red-500">Failed to load rooms.</p>
  }

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Available Rooms</h1>
      {rooms?.length === 0 ? (
        <p className="text-gray-400">No rooms available.</p>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {rooms?.map(room => <RoomCard key={room.id} room={room} />)}
        </div>
      )}
    </div>
  )
}
