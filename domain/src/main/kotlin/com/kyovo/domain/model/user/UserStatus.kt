package com.kyovo.domain.model.user

enum class UserStatus(val label: String)
{
    CREATED("CREATED"),
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");

    val allowedTransitions: Set<UserStatus> by lazy {
        when (this)
        {
            CREATED -> setOf(ACTIVE)
            ACTIVE -> setOf(INACTIVE)
            INACTIVE -> setOf(ACTIVE)
        }
    }

    fun canTransitionTo(target: UserStatus): Boolean = target in allowedTransitions
}