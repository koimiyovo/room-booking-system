package com.kyovo.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "user_status_history")
class UserStatusHistoryEntity(
    @Id
    val id: UUID,

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val status: String,

    @Column(nullable = false)
    val since: OffsetDateTime,

    @Column(nullable = true)
    val until: OffsetDateTime?
)
