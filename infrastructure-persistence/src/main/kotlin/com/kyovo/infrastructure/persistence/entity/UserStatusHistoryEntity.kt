package com.kyovo.infrastructure.persistence.entity

import com.kyovo.domain.model.user.UserStatus
import com.kyovo.domain.model.user.UserStatusInfo
import com.kyovo.domain.model.user.UserStatusInfoDate
import com.kyovo.domain.model.user.UserStatusReason
import com.kyovo.infrastructure.persistence.exception.InvalidUserStatusException
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "user_status_history")
class UserStatusHistoryEntity(
    @Id
    val id: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = ForeignKey(name = "fk_user_status_history_user_id"))
    val user: UserEntity,

    @Column(nullable = false)
    val status: String,

    @Column(nullable = false)
    val since: OffsetDateTime,

    @Column(nullable = true)
    val until: OffsetDateTime?,

    @Column(nullable = true)
    val reason: String?
)
{
    fun toStatusInfo(): UserStatusInfo
    {
        val parsedStatus = UserStatus.entries.firstOrNull { it.label == status }
            ?: throw InvalidUserStatusException(status)
        return UserStatusInfo(parsedStatus, UserStatusInfoDate(since), reason?.let { UserStatusReason(it) })
    }
}
