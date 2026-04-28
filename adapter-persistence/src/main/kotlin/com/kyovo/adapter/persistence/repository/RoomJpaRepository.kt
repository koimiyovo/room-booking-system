package com.kyovo.adapter.persistence.repository

import com.kyovo.adapter.persistence.entity.RoomEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RoomJpaRepository : JpaRepository<RoomEntity, UUID>
