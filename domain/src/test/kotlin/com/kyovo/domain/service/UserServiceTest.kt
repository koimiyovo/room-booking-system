package com.kyovo.domain.service

import com.kyovo.domain.model.*
import com.kyovo.domain.port.secondary.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class UserServiceTest {

    private val userRepository: UserRepository = mock()
    private val userService = UserService(userRepository)

    @Test
    fun `findAll returns all users from the repository`() {
        val users = listOf(User(UserId(UUID.randomUUID()), UserName("Alice"), UserEmail("alice@example.com")))
        whenever(userRepository.findAll()).thenReturn(users)

        val result = userService.findAll()

        assertThat(result).isEqualTo(users)
    }

    @Test
    fun `findAll returns an empty list when no users exist`() {
        whenever(userRepository.findAll()).thenReturn(emptyList())

        val result = userService.findAll()

        assertThat(result).isEmpty()
    }

    @Test
    fun `findById returns the user when it exists`() {
        val id = UserId(UUID.randomUUID())
        val user = User(id, UserName("Bob"), UserEmail("bob@example.com"))
        whenever(userRepository.findById(id)).thenReturn(user)

        val result = userService.findById(id)

        assertThat(result).isEqualTo(user)
    }

    @Test
    fun `findById returns null when the user does not exist`() {
        val id = UserId(UUID.randomUUID())
        whenever(userRepository.findById(id)).thenReturn(null)

        val result = userService.findById(id)

        assertThat(result).isNull()
    }

    @Test
    fun `save persists the user with a generated new identifier`() {
        val newUser = NewUser(UserName("Charlie"), UserEmail("charlie@example.com"))
        val captor = argumentCaptor<User>()
        whenever(userRepository.save(any())).thenAnswer { it.getArgument<User>(0) }

        userService.save(newUser)

        verify(userRepository).save(captor.capture())
        val captured = captor.firstValue
        assertThat(captured.name).isEqualTo(newUser.name)
        assertThat(captured.email).isEqualTo(newUser.email)
        assertThat(captured.id.value).isNotNull()
    }

    @Test
    fun `save returns the user persisted by the repository`() {
        val newUser = NewUser(UserName("Diana"), UserEmail("diana@example.com"))
        val savedUser = User(UserId(UUID.randomUUID()), UserName("Diana"), UserEmail("diana@example.com"))
        whenever(userRepository.save(any())).thenReturn(savedUser)

        val result = userService.save(newUser)

        assertThat(result).isEqualTo(savedUser)
    }
}
