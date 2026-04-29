package com.kyovo.adapter.web.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.kyovo.adapter.web.dto.CreateUserRequest
import com.kyovo.domain.model.*
import com.kyovo.domain.port.primary.UserUseCase
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

@WebMvcTest(UserController::class)
class UserControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var userUseCase: UserUseCase

    private val userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    private val user = User(UserId(userId), UserName("Alice"), UserEmail("alice@example.com"))

    @Test
    fun `GET api-users returns 200 with the list of users`() {
        whenever(userUseCase.findAll()).thenReturn(listOf(user))

        mockMvc.get("/api/users")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(userId.toString()) }
                jsonPath("$[0].name") { value("Alice") }
                jsonPath("$[0].email") { value("alice@example.com") }
            }
    }

    @Test
    fun `GET api-users returns 200 with an empty list`() {
        whenever(userUseCase.findAll()).thenReturn(emptyList())

        mockMvc.get("/api/users")
            .andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                content { json("[]") }
            }
    }

    @Test
    fun `GET api-users-id returns 200 with the user when it exists`() {
        whenever(userUseCase.findById(UserId(userId))).thenReturn(user)

        mockMvc.get("/api/users/$userId")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(userId.toString()) }
                jsonPath("$.name") { value("Alice") }
                jsonPath("$.email") { value("alice@example.com") }
            }
    }

    @Test
    fun `GET api-users-id returns 404 when the user does not exist`() {
        whenever(userUseCase.findById(UserId(userId))).thenReturn(null)

        mockMvc.get("/api/users/$userId")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `POST api-users returns 201 with the created user`() {
        val request = CreateUserRequest("Bob", "bob@example.com")
        val createdUser = User(UserId(userId), UserName("Bob"), UserEmail("bob@example.com"))
        whenever(userUseCase.save(any())).thenReturn(createdUser)

        mockMvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { value(userId.toString()) }
            jsonPath("$.name") { value("Bob") }
            jsonPath("$.email") { value("bob@example.com") }
        }
    }

    @Test
    fun `POST api-users calls the use case with the request body values`() {
        val request = CreateUserRequest("Charlie", "charlie@example.com")
        whenever(userUseCase.save(any())).thenReturn(
            User(UserId(userId), UserName("Charlie"), UserEmail("charlie@example.com"))
        )

        mockMvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
        }

        verify(userUseCase).save(NewUser(UserName("Charlie"), UserEmail("charlie@example.com")))
    }
}
