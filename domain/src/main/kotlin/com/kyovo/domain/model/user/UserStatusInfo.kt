package com.kyovo.domain.model.user

data class UserStatusInfo(
    val status: UserStatus,
    val since: UserStatusInfoDate
)
{
    fun isActive(): Boolean
    {
        return status == UserStatus.ACTIVE
    }
}
