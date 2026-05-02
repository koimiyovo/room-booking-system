package com.kyovo.domain.model.user

data class User(
    val id: UserId,
    val name: UserName,
    val email: UserEmail,
    val password: UserPassword,
    val role: UserRole,
    val registeredAt: UserRegistrationDate,
    val statusInfo: UserStatusInfo
)
{
    fun isInactive(): Boolean
    {
        return statusInfo.isInactive()
    }

    fun transitionTo(target: UserStatus, now: UserStatusInfoDate, reason: UserStatusReason?): User?
    {
        if (!statusInfo.status.canTransitionTo(target))
            return null
        return copy(statusInfo = UserStatusInfo(target, now, reason))
    }
}
