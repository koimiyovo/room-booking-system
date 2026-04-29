package com.kyovo.adapter.web.controller

import com.kyovo.adapter.web.dto.CreateRoomRequest
import com.kyovo.adapter.web.dto.CreateRoomResponse
import com.kyovo.domain.model.Room
import com.kyovo.domain.model.RoomId
import com.kyovo.domain.port.primary.RoomUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms", description = "Room management")
class RoomController(private val roomUseCase: RoomUseCase) {
    @GetMapping
    @Operation(summary = "List all rooms")
    @ApiResponse(responseCode = "200", description = "Room list returned successfully")
    fun findAll(): List<Room> {
        return roomUseCase.findAll()
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a room by its identifier")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Room found"),
        ApiResponse(responseCode = "404", description = "Room not found")
    )
    fun findById(
        @Parameter(description = "UUID identifier of the room")
        @PathVariable id: UUID
    ): ResponseEntity<Room> {
        return roomUseCase.findById(RoomId(id))
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @PostMapping
    @Operation(summary = "Create a new room")
    @ApiResponse(responseCode = "201", description = "Room created successfully")
    fun create(@RequestBody request: CreateRoomRequest): ResponseEntity<CreateRoomResponse> {
        val room = roomUseCase.save(request.toNewRoom())
        val response = CreateRoomResponse(
            id = room.id.value,
            name = room.name.value,
            capacity = room.capacity.value
        )
        return ResponseEntity(response, HttpStatus.CREATED)
    }
}
