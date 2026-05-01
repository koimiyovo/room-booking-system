package com.kyovo.domain.model

import com.kyovo.domain.exception.InvalidRoleException

enum class UserRole(val label: String)
{
    USER("USER"),
    ADMIN("ADMIN");

    companion object
    {
        fun from(value: String): UserRole
        {
            return when (value)
            {
                "ADMIN" -> ADMIN
                "USER" -> USER
                else -> throw InvalidRoleException(value)
            }
        }
    }
}
