package com.kyovo.adapter.web.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.kyovo.adapter.web.dto.CreateRoomRequest
import com.kyovo.domain.model.*
import com.kyovo.domain.port.primary.RoomUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

@WebMvcTest(RoomController::class)
class RoomControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var roomUseCase: RoomUseCase

    private val roomId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    private val room = Room(RoomId(roomId), RoomName("Salle A"), RoomCapacity(10))

    @Test
    fun `GET api-rooms retourne 200 avec la liste des salles`() {
        whenever(roomUseCase.findAll()).thenReturn(listOf(room))

        mockMvc.get("/api/rooms")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(roomId.toString()) }
                jsonPath("$[0].name") { value("Salle A") }
                jsonPath("$[0].capacity") { value(10) }
            }
    }

    @Test
    fun `GET api-rooms retourne 200 avec une liste vide`() {
        whenever(roomUseCase.findAll()).thenReturn(emptyList())

        mockMvc.get("/api/rooms")
            .andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                content { json("[]") }
            }
    }

    @Test
    fun `GET api-rooms-id retourne 200 avec la salle quand elle existe`() {
        whenever(roomUseCase.findById(RoomId(roomId))).thenReturn(room)

        mockMvc.get("/api/rooms/$roomId")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(roomId.toString()) }
                jsonPath("$.name") { value("Salle A") }
                jsonPath("$.capacity") { value(10) }
            }
    }

    @Test
    fun `GET api-rooms-id retourne 404 quand la salle n'existe pas`() {
        whenever(roomUseCase.findById(RoomId(roomId))).thenReturn(null)

        mockMvc.get("/api/rooms/$roomId")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `POST api-rooms retourne 201 avec la salle créée`() {
        val request = CreateRoomRequest("Salle B", 20)
        val createdRoom = Room(RoomId(roomId), RoomName("Salle B"), RoomCapacity(20))
        whenever(roomUseCase.save(any())).thenReturn(createdRoom)

        mockMvc.post("/api/rooms") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { value(roomId.toString()) }
            jsonPath("$.name") { value("Salle B") }
            jsonPath("$.capacity") { value(20) }
        }
    }

    @Test
    fun `POST api-rooms appelle le use case avec les valeurs du body`() {
        val request = CreateRoomRequest("Salle C", 30)
        whenever(roomUseCase.save(any())).thenReturn(
            Room(RoomId(roomId), RoomName("Salle C"), RoomCapacity(30))
        )

        mockMvc.post("/api/rooms") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
        }

        verify(roomUseCase).save(NewRoom(RoomName("Salle C"), RoomCapacity(30)))
    }
}
