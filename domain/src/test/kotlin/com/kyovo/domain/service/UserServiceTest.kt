package com.kyovo.domain.service

import com.kyovo.domain.exception.EmailAlreadyUsedException
import com.kyovo.domain.exception.UserNotFoundException
import com.kyovo.domain.model.user.*
import com.kyovo.domain.port.secondary.PasswordHashPort
import com.kyovo.domain.port.secondary.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime
import java.util.*

class UserServiceTest
{
    private val userRepository: UserRepository = mock()
    private val passwordHashPort: PasswordHashPort = mock()
    private val userService = UserService(userRepository, passwordHashPort)

    private val userId = UserId(UUID.randomUUID())
    private val existingUser = User(
        userId,
        UserName("Alice"),
        UserEmail("alice@example.com"),
        UserPassword("hashed"),
        UserRole.USER,
        UserRegistrationDate(OffsetDateTime.now())
    )

    @Test
    fun `findAll returns all users from the repository`()
    {
        whenever(userRepository.findAll()).thenReturn(listOf(existingUser))

        val result = userService.findAll()

        assertThat(result).isEqualTo(listOf(existingUser))
    }

    @Test
    fun `findAll returns an empty list when no users exist`()
    {
        whenever(userRepository.findAll()).thenReturn(emptyList())

        val result = userService.findAll()

        assertThat(result).isEmpty()
    }

    @Test
    fun `findById returns the user when it exists`()
    {
        whenever(userRepository.findById(userId)).thenReturn(existingUser)

        val result = userService.findById(userId)

        assertThat(result).isEqualTo(existingUser)
    }

    @Test
    fun `findById returns null when the user does not exist`()
    {
        whenever(userRepository.findById(userId)).thenReturn(null)

        val result = userService.findById(userId)

        assertThat(result).isNull()
    }

    @Test
    fun `update changes name and email when both are provided`()
    {
        val data = UpdateUser(UserName("New Name"), UserEmail("new@example.com"), null)
        val newEmail = UserEmail("new@example.com")
        whenever(userRepository.findById(userId)).thenReturn(existingUser)
        whenever(userRepository.findByEmail(newEmail)).thenReturn(null)
        whenever(userRepository.save(any())).thenAnswer { it.getArgument<User>(0) }

        val result = userService.update(userId, data)

        assertThat(result.name).isEqualTo(UserName("New Name"))
        assertThat(result.email).isEqualTo(UserEmail("new@example.com"))
    }

    @Test
    fun `update throws UserNotFoundException when user does not exist`()
    {
        whenever(userRepository.findById(userId)).thenReturn(null)

        assertThatThrownBy { userService.update(userId, UpdateUser(null, null, null)) }
            .isInstanceOf(UserNotFoundException::class.java)
    }

    @Test
    fun `update throws EmailAlreadyUsedException when new email is taken by another user`()
    {
        val otherUser = existingUser.copy(id = UserId(UUID.randomUUID()), email = UserEmail("taken@example.com"))
        val data = UpdateUser(null, UserEmail("taken@example.com"), null)
        whenever(userRepository.findById(userId)).thenReturn(existingUser)
        whenever(userRepository.findByEmail(UserEmail("taken@example.com"))).thenReturn(otherUser)

        assertThatThrownBy { userService.update(userId, data) }
            .isInstanceOf(EmailAlreadyUsedException::class.java)
    }

    @Test
    fun `delete removes the user when it exists`()
    {
        whenever(userRepository.findById(userId)).thenReturn(existingUser)

        userService.delete(userId)

        verify(userRepository).deleteById(userId)
    }

    @Test
    fun `delete throws UserNotFoundException when user does not exist`()
    {
        whenever(userRepository.findById(userId)).thenReturn(null)

        assertThatThrownBy { userService.delete(userId) }
            .isInstanceOf(UserNotFoundException::class.java)
    }
}
