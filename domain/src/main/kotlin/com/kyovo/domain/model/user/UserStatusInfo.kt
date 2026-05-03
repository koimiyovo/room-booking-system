package com.kyovo.domain.model.user

data class UserStatusInfo(
    val status: UserStatus,
    val since: UserStatusInfoDate,
    val reason: UserStatusReason?
)
{
    fun isInactive(): Boolean
    {
        return status == UserStatus.INACTIVE
    }
}
