package com.kyovo.infrastructure.api.dto

import com.kyovo.domain.model.user.User
import java.time.OffsetDateTime
import java.util.*

data class UserResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val role: UserRoleResponse,
    val registeredAt: OffsetDateTime,
    val statusInfo: UserStatusInfoResponse
)
{
    companion object
    {
        fun fromDomain(user: User): UserResponse
        {
            return UserResponse(
                id = user.id.value,
                name = user.name.value,
                email = user.email.value,
                role = UserRoleResponse.from(user.role),
                registeredAt = user.registeredAt.value,
                statusInfo = UserStatusInfoResponse(
                    status = UserStatusResponse.from(user.statusInfo.status),
                    since = user.statusInfo.since.value,
                    reason = user.statusInfo.reason?.value
                )
            )
        }
    }
}
