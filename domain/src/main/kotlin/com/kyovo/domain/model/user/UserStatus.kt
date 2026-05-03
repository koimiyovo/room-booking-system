package com.kyovo.domain.model.user

enum class UserStatus(val label: String)
{
    CREATED("CREATED"),
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    DELETED("DELETED");

    val allowedTransitions: Set<UserStatus> by lazy {
        when (this)
        {
            CREATED -> setOf(ACTIVE, DELETED)
            ACTIVE -> setOf(INACTIVE, DELETED)
            INACTIVE -> setOf(ACTIVE, DELETED)
            DELETED -> emptySet()
        }
    }

    fun canTransitionTo(target: UserStatus): Boolean = target in allowedTransitions
}