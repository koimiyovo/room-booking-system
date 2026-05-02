package com.kyovo.infrastructure.persistence.entity

import com.kyovo.domain.model.user.UserStatus
import com.kyovo.domain.model.user.UserStatusInfo
import com.kyovo.domain.model.user.UserStatusInfoDate
import com.kyovo.infrastructure.persistence.exception.InvalidUserStatusException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "user_status_history")
class UserStatusHistoryEntity(
    @Id
    val id: UUID,

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val status: String,

    @Column(nullable = false)
    val since: OffsetDateTime,

    @Column(nullable = true)
    val until: OffsetDateTime?
)
{
    fun toStatusInfo(): UserStatusInfo
    {
        val parsedStatus = UserStatus.entries.firstOrNull { it.label == status }
            ?: throw InvalidUserStatusException(status)
        return UserStatusInfo(parsedStatus, UserStatusInfoDate(since))
    }
}
