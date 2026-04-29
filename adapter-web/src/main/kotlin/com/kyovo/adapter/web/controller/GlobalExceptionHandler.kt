package com.kyovo.adapter.web.controller

import com.kyovo.domain.exception.BookingConflictException
import com.kyovo.domain.exception.RoomCapacityExceededException
import com.kyovo.domain.exception.RoomNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler
{
    @ExceptionHandler(RoomNotFoundException::class)
    fun handleRoomNotFound(ex: RoomNotFoundException): ResponseEntity<String>
    {
        return ResponseEntity(ex.message, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(RoomCapacityExceededException::class)
    fun handleCapacityExceeded(ex: RoomCapacityExceededException): ResponseEntity<String>
    {
        return ResponseEntity(ex.message, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(BookingConflictException::class)
    fun handleBookingConflict(ex: BookingConflictException): ResponseEntity<String>
    {
        return ResponseEntity(ex.message, HttpStatus.CONFLICT)
    }
}
