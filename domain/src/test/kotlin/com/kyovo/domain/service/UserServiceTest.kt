package com.kyovo.domain.service

import com.kyovo.domain.exception.AccountNotOwnedByUserException
import com.kyovo.domain.exception.EmailAlreadyUsedException
import com.kyovo.domain.exception.InvalidStatusTransitionException
import com.kyovo.domain.exception.UserNotFoundException
import com.kyovo.domain.model.user.*
import com.kyovo.domain.port.secondary.ClockPort
import com.kyovo.domain.port.secondary.PasswordHashPort
import com.kyovo.domain.port.secondary.TransactionPort
import com.kyovo.domain.port.secondary.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.OffsetDateTime
import java.util.*

class UserServiceTest
{
    private val userRepository: UserRepository = mock()
    private val passwordHashPort: PasswordHashPort = mock()
    private val transactionPort: TransactionPort = mock()
    private val clockPort: ClockPort = mock()
    private val userService = UserService(userRepository, passwordHashPort, transactionPort, clockPort)

    private val userId = UserId(UUID.randomUUID())
    private val existingUser = User(
        userId,
        UserName("Alice"),
        UserEmail("alice@example.com"),
        UserPassword("hashed"),
        UserRole.USER,
        UserRegistrationDate(OffsetDateTime.now()),
        UserStatusInfo(status = UserStatus.CREATED, since = UserStatusInfoDate(OffsetDateTime.now()))
    )
    private val activeUser = existingUser.copy(
        statusInfo = UserStatusInfo(status = UserStatus.ACTIVE, since = UserStatusInfoDate(OffsetDateTime.now()))
    )
    private val inactiveUser = existingUser.copy(
        statusInfo = UserStatusInfo(status = UserStatus.INACTIVE, since = UserStatusInfoDate(OffsetDateTime.now()))
    )

    @BeforeEach
    fun setUp()
    {
        whenever(clockPort.now()).thenReturn(OffsetDateTime.now())
        doAnswer { (it.arguments[0] as Function0<*>).invoke() }
            .whenever(transactionPort).executeInTransaction<Any>(any<() -> Any>())
    }

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

    @Test
    fun `validate changes the user status to ACTIVE when it exists`()
    {
        whenever(userRepository.findById(userId)).thenReturn(existingUser)
        whenever(userRepository.update(any())).thenReturn(activeUser)

        val result = userService.validate(userId, isAdmin = false, validateBy = userId)

        assertThat(result.statusInfo.status).isEqualTo(UserStatus.ACTIVE)
    }

    @Test
    fun `validate records status history after successful transition`()
    {
        whenever(userRepository.findById(userId)).thenReturn(existingUser)
        whenever(userRepository.update(any())).thenReturn(activeUser)

        userService.validate(userId, isAdmin = false, validateBy = userId)

        verify(userRepository).saveStatusHistory(activeUser.id, UserStatus.ACTIVE, activeUser.statusInfo.since)
    }

    @Test
    fun `validate throws UserNotFoundException when user does not exist`()
    {
        whenever(userRepository.findById(userId)).thenReturn(null)

        assertThatThrownBy { userService.validate(userId, isAdmin = false, validateBy = userId) }
            .isInstanceOf(UserNotFoundException::class.java)
    }

    @Test
    fun `validate throws AccountNotOwnedByUserException when non-admin user validates another account`()
    {
        val otherId = UserId(UUID.randomUUID())
        whenever(userRepository.findById(userId)).thenReturn(existingUser)

        assertThatThrownBy { userService.validate(userId, isAdmin = false, validateBy = otherId) }
            .isInstanceOf(AccountNotOwnedByUserException::class.java)
    }

    @Test
    fun `validate throws InvalidStatusTransitionException when account is already active`()
    {
        whenever(userRepository.findById(userId)).thenReturn(activeUser)

        assertThatThrownBy { userService.validate(userId, isAdmin = false, validateBy = userId) }
            .isInstanceOf(InvalidStatusTransitionException::class.java)
    }

    @Test
    fun `deactivate changes the user status to INACTIVE when account is active`()
    {
        whenever(userRepository.findById(userId)).thenReturn(activeUser)
        whenever(userRepository.update(any())).thenReturn(inactiveUser)

        val result = userService.deactivate(userId)

        assertThat(result.statusInfo.status).isEqualTo(UserStatus.INACTIVE)
    }

    @Test
    fun `deactivate records status history after successful transition`()
    {
        whenever(userRepository.findById(userId)).thenReturn(activeUser)
        whenever(userRepository.update(any())).thenReturn(inactiveUser)

        userService.deactivate(userId)

        verify(userRepository).saveStatusHistory(inactiveUser.id, UserStatus.INACTIVE, inactiveUser.statusInfo.since)
    }

    @Test
    fun `deactivate throws UserNotFoundException when user does not exist`()
    {
        whenever(userRepository.findById(userId)).thenReturn(null)

        assertThatThrownBy { userService.deactivate(userId) }
            .isInstanceOf(UserNotFoundException::class.java)
    }

    @Test
    fun `deactivate throws InvalidStatusTransitionException when account is not active`()
    {
        whenever(userRepository.findById(userId)).thenReturn(existingUser)

        assertThatThrownBy { userService.deactivate(userId) }
            .isInstanceOf(InvalidStatusTransitionException::class.java)
    }

    @Test
    fun `reactivate changes the user status to ACTIVE when account is inactive`()
    {
        whenever(userRepository.findById(userId)).thenReturn(inactiveUser)
        whenever(userRepository.update(any())).thenReturn(activeUser)

        val result = userService.reactivate(userId)

        assertThat(result.statusInfo.status).isEqualTo(UserStatus.ACTIVE)
    }

    @Test
    fun `reactivate records status history after successful transition`()
    {
        whenever(userRepository.findById(userId)).thenReturn(inactiveUser)
        whenever(userRepository.update(any())).thenReturn(activeUser)

        userService.reactivate(userId)

        verify(userRepository).saveStatusHistory(activeUser.id, UserStatus.ACTIVE, activeUser.statusInfo.since)
    }

    @Test
    fun `reactivate throws UserNotFoundException when user does not exist`()
    {
        whenever(userRepository.findById(userId)).thenReturn(null)

        assertThatThrownBy { userService.reactivate(userId) }
            .isInstanceOf(UserNotFoundException::class.java)
    }

    @Test
    fun `reactivate throws InvalidStatusTransitionException when account is not inactive`()
    {
        whenever(userRepository.findById(userId)).thenReturn(activeUser)

        assertThatThrownBy { userService.reactivate(userId) }
            .isInstanceOf(InvalidStatusTransitionException::class.java)
    }
}
