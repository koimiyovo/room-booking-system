package com.kyovo.infrastructure.persistence.entity

import com.kyovo.domain.model.room.Room
import com.kyovo.domain.model.room.RoomCapacity
import com.kyovo.domain.model.room.RoomId
import com.kyovo.domain.model.room.RoomName
import com.kyovo.domain.model.user.UserId
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "rooms")
class RoomEntity(
    @Id
    val id: UUID,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val capacity: Int,

    @Column(name = "requires_validation", nullable = false)
    val requiresValidation: Boolean,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, foreignKey = ForeignKey(name = "fk_room_created_by"))
    val createdByUser: UserEntity
)
{
    companion object
    {
        fun fromDomain(room: Room, createdByUser: UserEntity): RoomEntity
        {
            return RoomEntity(
                id = room.id.value,
                name = room.name.value,
                capacity = room.capacity.value,
                requiresValidation = room.requiresValidation,
                createdByUser = createdByUser
            )
        }
    }

    fun toDomain(): Room
    {
        return Room(
            id = RoomId(id),
            name = RoomName(name),
            capacity = RoomCapacity(capacity),
            requiresValidation = requiresValidation,
            createdBy = UserId(createdByUser.id)
        )
    }
}
