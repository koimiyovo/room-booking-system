import type { Room } from '@/domain/model/Room'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

interface Props {
  room: Room
}

export default function RoomCard({ room }: Props) {
  return (
    <Card className="hover:shadow-md transition-shadow">
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between">
          <CardTitle className="text-base">{room.name}</CardTitle>
          {room.requiresValidation && (
            <Badge variant="secondary">Requires validation</Badge>
          )}
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-gray-500">Capacity: {room.capacity} people</p>
      </CardContent>
    </Card>
  )
}
