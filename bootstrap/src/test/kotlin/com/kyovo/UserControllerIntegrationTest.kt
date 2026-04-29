package com.kyovo

import com.fasterxml.jackson.databind.ObjectMapper
import com.kyovo.adapter.persistence.repository.UserJpaRepository
import com.kyovo.adapter.web.dto.CreateUserRequest
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
class UserControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @BeforeEach
    fun setUp() {
        userJpaRepository.deleteAll()
    }

    @Test
    fun `GET api-users returns 200 with empty list when no users exist`() {
        mockMvc.get("/api/users")
            .andExpect {
                status { isOk() }
                content { json("[]") }
            }
    }

    @Test
    fun `POST api-users returns 201 with the created user`() {
        mockMvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateUserRequest("Alice", "alice@example.com"))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNotEmpty() }
            jsonPath("$.name") { value("Alice") }
            jsonPath("$.email") { value("alice@example.com") }
        }
    }

    @Test
    fun `POST then GET api-users returns the created user in the list`() {
        val postResult = mockMvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateUserRequest("Bob", "bob@example.com"))
        }.andReturn()

        val createdId = objectMapper.readTree(postResult.response.contentAsString)["id"].asText()

        mockMvc.get("/api/users")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(createdId) }
                jsonPath("$[0].name") { value("Bob") }
                jsonPath("$[0].email") { value("bob@example.com") }
            }

        assertThat(userJpaRepository.count()).isEqualTo(1)
    }

    @Test
    fun `POST then GET api-users-id returns the user by its identifier`() {
        val postResult = mockMvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateUserRequest("Charlie", "charlie@example.com"))
        }.andReturn()

        val createdId = objectMapper.readTree(postResult.response.contentAsString)["id"].asText()

        mockMvc.get("/api/users/$createdId")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(createdId) }
                jsonPath("$.name") { value("Charlie") }
                jsonPath("$.email") { value("charlie@example.com") }
            }
    }

    @Test
    fun `GET api-users-id returns 404 for a non-existent identifier`() {
        mockMvc.get("/api/users/${UUID.randomUUID()}")
            .andExpect {
                status { isNotFound() }
            }
    }
}
