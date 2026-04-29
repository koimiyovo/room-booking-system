package com.kyovo.adapter.persistence.entity

import com.kyovo.domain.model.User
import com.kyovo.domain.model.UserEmail
import com.kyovo.domain.model.UserId
import com.kyovo.domain.model.UserName
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    val id: UUID,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val email: String
) {
    companion object {
        fun fromDomain(user: User): UserEntity {
            return UserEntity(
                id = user.id.value,
                name = user.name.value,
                email = user.email.value
            )
        }
    }

    fun toDomain(): User {
        return User(
            id = UserId(id),
            name = UserName(name),
            email = UserEmail(email)
        )
    }
}
