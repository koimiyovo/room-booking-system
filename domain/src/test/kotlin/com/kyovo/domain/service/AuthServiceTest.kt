package com.kyovo.domain.service

import com.kyovo.domain.exception.EmailAlreadyUsedException
import com.kyovo.domain.exception.InvalidCredentialsException
import com.kyovo.domain.model.*
import com.kyovo.domain.port.secondary.PasswordHashPort
import com.kyovo.domain.port.secondary.UserRepository
import com.kyovo.domain.provider.TimeProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.*

class AuthServiceTest
{
    private val userRepository: UserRepository = mock()
    private val passwordHashPort: PasswordHashPort = mock()
    private val timeProvider = object : TimeProvider
    {
        override fun now(): LocalDateTime
        {
            return LocalDateTime.of(2026, 4, 1, 0, 0)
        }
    }
    private val authService = AuthService(userRepository, passwordHashPort, timeProvider)

    private val userId = UserId(UUID.randomUUID())
    private val existingUser = User(
        userId,
        UserName("Alice"),
        UserEmail("alice@example.com"),
        UserPassword("hashed"),
        UserRole.USER,
        UserRegistrationDate(timeProvider.now())
    )

    @Test
    fun `register hashes the password before persisting`()
    {
        val newUser = NewUser(UserName("Alice"), UserEmail("alice@example.com"), UserPassword("plain"))
        val captor = argumentCaptor<User>()
        whenever(userRepository.findByEmail(newUser.email)).thenReturn(null)
        whenever(passwordHashPort.hash("plain")).thenReturn(UserPassword("hashed"))
        whenever(userRepository.save(any())).thenAnswer { it.getArgument<User>(0) }

        authService.register(newUser)

        verify(userRepository).save(captor.capture())
        assertThat(captor.firstValue.password).isEqualTo(UserPassword("hashed"))
    }

    @Test
    fun `register returns the user persisted by the repository`()
    {
        val newUser = NewUser(UserName("Alice"), UserEmail("alice@example.com"), UserPassword("plain"))
        whenever(userRepository.findByEmail(newUser.email)).thenReturn(null)
        whenever(passwordHashPort.hash("plain")).thenReturn(UserPassword("hashed"))
        whenever(userRepository.save(any())).thenReturn(existingUser)

        val result = authService.register(newUser)

        assertThat(result).isEqualTo(existingUser)
    }

    @Test
    fun `register throws EmailAlreadyUsedException when email is taken`()
    {
        val newUser = NewUser(UserName("Dup"), UserEmail("alice@example.com"), UserPassword("plain"))
        whenever(userRepository.findByEmail(newUser.email)).thenReturn(existingUser)

        assertThatThrownBy { authService.register(newUser) }
            .isInstanceOf(EmailAlreadyUsedException::class.java)
    }

    @Test
    fun `login returns the user when credentials are valid`()
    {
        whenever(userRepository.findByEmail(UserEmail("alice@example.com"))).thenReturn(existingUser)
        whenever(passwordHashPort.matches("plain", UserPassword("hashed"))).thenReturn(true)

        val result = authService.login(UserEmail("alice@example.com"), "plain")

        assertThat(result).isEqualTo(existingUser)
    }

    @Test
    fun `login throws InvalidCredentialsException when email is unknown`()
    {
        whenever(userRepository.findByEmail(any())).thenReturn(null)

        assertThatThrownBy { authService.login(UserEmail("unknown@example.com"), "plain") }
            .isInstanceOf(InvalidCredentialsException::class.java)
    }

    @Test
    fun `login throws InvalidCredentialsException when password is wrong`()
    {
        whenever(userRepository.findByEmail(UserEmail("alice@example.com"))).thenReturn(existingUser)
        whenever(passwordHashPort.matches("wrong", UserPassword("hashed"))).thenReturn(false)

        assertThatThrownBy { authService.login(UserEmail("alice@example.com"), "wrong") }
            .isInstanceOf(InvalidCredentialsException::class.java)
    }
}
