package com.kyovo.domain.model.user

data class UserStatusInfo(
    val status: UserStatus,
    val since: UserStatusInfoDate
)
{
    fun isInactive(): Boolean
    {
        return status == UserStatus.INACTIVE
    }
}
