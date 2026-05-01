package com.kyovo.adapter.persistence.entity

import com.kyovo.domain.model.*
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    val id: UUID,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val password: String,

    @Column(nullable = false)
    val role: String,

    @Column(nullable = false)
    val registeredAt: LocalDateTime
)
{
    companion object
    {
        fun fromDomain(user: User): UserEntity
        {
            return UserEntity(
                id = user.id.value,
                name = user.name.value,
                email = user.email.value,
                password = user.password.value,
                role = user.role.label,
                registeredAt = user.registeredAt.value
            )
        }
    }

    fun toDomain(): User
    {
        return User(
            id = UserId(id),
            name = UserName(name),
            email = UserEmail(email),
            password = UserPassword(password),
            role = UserRole.valueOf(role), //TODO secure
            registeredAt = UserRegistrationDate(registeredAt)
        )
    }
}
