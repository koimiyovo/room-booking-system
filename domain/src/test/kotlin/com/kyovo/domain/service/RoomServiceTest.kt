package com.kyovo.domain.service

import com.kyovo.domain.model.*
import com.kyovo.domain.port.secondary.RoomRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class RoomServiceTest {

    private val roomRepository: RoomRepository = mock()
    private val roomService = RoomService(roomRepository)

    @Test
    fun `findAll returns all rooms from the repository`() {
        val rooms = listOf(Room(RoomId(UUID.randomUUID()), RoomName("Salle A"), RoomCapacity(10)))
        whenever(roomRepository.findAll()).thenReturn(rooms)

        val result = roomService.findAll()

        assertThat(result).isEqualTo(rooms)
    }

    @Test
    fun `findAll returns an empty list when no rooms exist`() {
        whenever(roomRepository.findAll()).thenReturn(emptyList())

        val result = roomService.findAll()

        assertThat(result).isEmpty()
    }

    @Test
    fun `findById returns the room when it exists`() {
        val id = RoomId(UUID.randomUUID())
        val room = Room(id, RoomName("Salle B"), RoomCapacity(5))
        whenever(roomRepository.findById(id)).thenReturn(room)

        val result = roomService.findById(id)

        assertThat(result).isEqualTo(room)
    }

    @Test
    fun `findById returns null when the room does not exist`() {
        val id = RoomId(UUID.randomUUID())
        whenever(roomRepository.findById(id)).thenReturn(null)

        val result = roomService.findById(id)

        assertThat(result).isNull()
    }

    @Test
    fun `save persists the room with a generated new identifier`() {
        val newRoom = NewRoom(RoomName("Salle C"), RoomCapacity(20))
        val captor = argumentCaptor<Room>()
        whenever(roomRepository.save(any())).thenAnswer { it.getArgument<Room>(0) }

        roomService.save(newRoom)

        verify(roomRepository).save(captor.capture())
        val captured = captor.firstValue
        assertThat(captured.name).isEqualTo(newRoom.name)
        assertThat(captured.capacity).isEqualTo(newRoom.capacity)
        assertThat(captured.id.value).isNotNull()
    }

    @Test
    fun `save returns the room persisted by the repository`() {
        val newRoom = NewRoom(RoomName("Salle D"), RoomCapacity(15))
        val savedRoom = Room(RoomId(UUID.randomUUID()), RoomName("Salle D"), RoomCapacity(15))
        whenever(roomRepository.save(any())).thenReturn(savedRoom)

        val result = roomService.save(newRoom)

        assertThat(result).isEqualTo(savedRoom)
    }
}
