package com.kyovo

import com.fasterxml.jackson.databind.ObjectMapper
import com.kyovo.adapter.persistence.repository.RoomJpaRepository
import com.kyovo.adapter.web.dto.CreateRoomRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class RoomControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var roomJpaRepository: RoomJpaRepository

    @BeforeEach
    fun setUp() {
        roomJpaRepository.deleteAll()
    }

    @Test
    fun `GET api-rooms returns 200 with empty list when no rooms exist`() {
        mockMvc.get("/api/rooms")
            .andExpect {
                status { isOk() }
                content { json("[]") }
            }
    }

    @Test
    fun `POST api-rooms returns 201 with the created room`() {
        mockMvc.post("/api/rooms") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateRoomRequest("Salle Conférence", 25))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNotEmpty() }
            jsonPath("$.name") { value("Salle Conférence") }
            jsonPath("$.capacity") { value(25) }
        }
    }

    @Test
    fun `POST then GET api-rooms returns the created room in the list`() {
        val postResult = mockMvc.post("/api/rooms") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateRoomRequest("Salle Réunion", 12))
        }.andReturn()

        val createdId = objectMapper.readTree(postResult.response.contentAsString)["id"].asText()

        mockMvc.get("/api/rooms")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(createdId) }
                jsonPath("$[0].name") { value("Salle Réunion") }
                jsonPath("$[0].capacity") { value(12) }
            }

        assertThat(roomJpaRepository.count()).isEqualTo(1)
    }

    @Test
    fun `POST then GET api-rooms-id returns the room by its identifier`() {
        val postResult = mockMvc.post("/api/rooms") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateRoomRequest("Salle Formation", 30))
        }.andReturn()

        val createdId = objectMapper.readTree(postResult.response.contentAsString)["id"].asText()

        mockMvc.get("/api/rooms/$createdId")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(createdId) }
                jsonPath("$.name") { value("Salle Formation") }
                jsonPath("$.capacity") { value(30) }
            }
    }

    @Test
    fun `GET api-rooms-id returns 404 for a non-existent identifier`() {
        mockMvc.get("/api/rooms/${UUID.randomUUID()}")
            .andExpect {
                status { isNotFound() }
            }
    }
}
