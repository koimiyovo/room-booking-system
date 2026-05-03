package com.kyovo.infrastructure.api.controller

import com.kyovo.domain.model.room.*
import com.kyovo.domain.port.primary.RoomUseCase
import com.kyovo.domain.port.primary.UserUseCase
import com.kyovo.infrastructure.api.dto.CreateRoomRequest
import com.kyovo.infrastructure.api.security.JwtService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.util.*

@WebMvcTest(RoomController::class)
@WithMockUser(roles = ["ADMIN"])
class RoomControllerWebMvcTest
{
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var roomUseCase: RoomUseCase

    @MockitoBean
    private lateinit var userUseCase: UserUseCase

    @MockitoBean
    private lateinit var jwtService: JwtService

    private val roomId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    private val room = Room(RoomId(roomId), RoomName("Salle A"), RoomCapacity(10))

    @Test
    fun `GET api-rooms returns 200 with the list of rooms`()
    {
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
    fun `GET api-rooms returns 200 with an empty list`()
    {
        whenever(roomUseCase.findAll()).thenReturn(emptyList())

        mockMvc.get("/api/rooms")
            .andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                content { json("[]") }
            }
    }

    @Test
    fun `GET api-rooms-id returns 200 with the room when it exists`()
    {
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
    fun `GET api-rooms-id returns 404 when the room does not exist`()
    {
        whenever(roomUseCase.findById(RoomId(roomId))).thenReturn(null)

        mockMvc.get("/api/rooms/$roomId")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `POST api-rooms returns 201 with the created room`()
    {
        val request = CreateRoomRequest("Salle B", 20)
        val createdRoom = Room(RoomId(roomId), RoomName("Salle B"), RoomCapacity(20))
        whenever(roomUseCase.save(any())).thenReturn(createdRoom)

        mockMvc.post("/api/rooms") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(csrf())
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { value(roomId.toString()) }
            jsonPath("$.name") { value("Salle B") }
            jsonPath("$.capacity") { value(20) }
        }
    }

    @Test
    fun `POST api-rooms calls the use case with the request body values`()
    {
        val request = CreateRoomRequest("Salle C", 30)
        whenever(roomUseCase.save(any())).thenReturn(
            Room(RoomId(roomId), RoomName("Salle C"), RoomCapacity(30))
        )

        mockMvc.post("/api/rooms") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(csrf())
        }.andExpect {
            status { isCreated() }
        }

        verify(roomUseCase).save(NewRoom(RoomName("Salle C"), RoomCapacity(30)))
    }
}
