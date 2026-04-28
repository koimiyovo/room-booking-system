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
@Tag(name = "Rooms", description = "Gestion des salles")
class RoomController(private val roomUseCase: RoomUseCase) {
    @GetMapping
    @Operation(summary = "Lister toutes les salles")
    @ApiResponse(responseCode = "200", description = "Liste des salles retournée avec succès")
    fun findAll(): List<Room> {
        return roomUseCase.findAll()
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une salle par son identifiant")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Salle trouvée"),
        ApiResponse(responseCode = "404", description = "Salle introuvable")
    )
    fun findById(
        @Parameter(description = "Identifiant UUID de la salle")
        @PathVariable id: UUID
    ): ResponseEntity<Room> {
        return roomUseCase.findById(RoomId(id))
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @PostMapping
    @Operation(summary = "Créer une nouvelle salle")
    @ApiResponse(responseCode = "201", description = "Salle créée avec succès")
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
