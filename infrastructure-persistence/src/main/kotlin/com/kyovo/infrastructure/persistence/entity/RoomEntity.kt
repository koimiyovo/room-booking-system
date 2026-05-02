package com.kyovo.infrastructure.persistence.entity

import com.kyovo.domain.model.room.Room
import com.kyovo.domain.model.room.RoomCapacity
import com.kyovo.domain.model.room.RoomId
import com.kyovo.domain.model.room.RoomName
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "rooms")
class RoomEntity(
    @Id
    val id: UUID,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val capacity: Int
)
{
    companion object
    {
        fun fromDomain(room: Room): RoomEntity
        {
            return RoomEntity(
                id = room.id.value,
                name = room.name.value,
                capacity = room.capacity.value
            )
        }
    }

    fun toDomain(): Room
    {
        return Room(
            id = RoomId(id),
            name = RoomName(name),
            capacity = RoomCapacity(capacity)
        )
    }
}
