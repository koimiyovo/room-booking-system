package com.kyovo

import com.kyovo.adapter.persistence.entity.UserEntity
import com.kyovo.adapter.persistence.repository.UserJpaRepository
import com.kyovo.adapter.web.dto.LoginRequest
import com.kyovo.adapter.web.dto.RegisterRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest
{
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setUp()
    {
        userJpaRepository.deleteAll()
    }

    @Test
    fun `POST api-auth-register creates a user and returns 201`()
    {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Alice", "alice@example.com", "password"))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNotEmpty() }
            jsonPath("$.name") { value("Alice") }
            jsonPath("$.email") { value("alice@example.com") }
            jsonPath("$.role") { value("USER") }
        }

        assertThat(userJpaRepository.count()).isEqualTo(1)
    }

    @Test
    fun `POST api-auth-register returns 409 when email is already taken`()
    {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Alice", "alice@example.com", "password"))
        }.andExpect { status { isCreated() } }

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RegisterRequest("Alice2", "alice@example.com", "other"))
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `POST api-auth-login returns 200 with a valid token`()
    {
        userJpaRepository.save(
            UserEntity(UUID.randomUUID(), "Alice", "alice@example.com", passwordEncoder.encode("password")!!, "USER")
        )

        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("alice@example.com", "password"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { isNotEmpty() }
        }.andReturn()

        val token = objectMapper.readTree(result.response.contentAsString)["token"].asText()
        assertThat(token).isNotBlank()
    }

    @Test
    fun `POST api-auth-login returns 401 when password is wrong`()
    {
        userJpaRepository.save(
            UserEntity(UUID.randomUUID(), "Alice", "alice@example.com", passwordEncoder.encode("password")!!, "USER")
        )

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("alice@example.com", "wrong"))
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `POST api-auth-login returns 401 when email is unknown`()
    {
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("unknown@example.com", "password"))
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
