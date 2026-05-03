package com.kyovo.infrastructure.api.controller

import com.kyovo.domain.exception.*
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

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(ex: UserNotFoundException): ResponseEntity<String>
    {
        return ResponseEntity(ex.message, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(EmailAlreadyUsedException::class)
    fun handleEmailAlreadyUsed(ex: EmailAlreadyUsedException): ResponseEntity<String>
    {
        return ResponseEntity(ex.message, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ResponseEntity<String>
    {
        return ResponseEntity(ex.message, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(BookingNotFoundException::class)
    fun handleBookingNotFound(ex: BookingNotFoundException): ResponseEntity<String>
    {
        return ResponseEntity(ex.message, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(BookingNotOwnedByUserException::class)
    fun handleBookingNotOwned(ex: BookingNotOwnedByUserException): ResponseEntity<String>
    {
        return ResponseEntity(ex.message, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(BookingAlreadyCancelledException::class)
    fun handleBookingAlreadyCancelled(ex: BookingAlreadyCancelledException): ResponseEntity<String>
    {
        return ResponseEntity(ex.message, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(AccountNotOwnedByUserException::class)
    fun handleAccountNotOwned(ex: AccountNotOwnedByUserException): ResponseEntity<String>
    {
        return ResponseEntity(ex.message, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(InvalidStatusTransitionException::class)
    fun handleInvalidStatusTransition(ex: InvalidStatusTransitionException): ResponseEntity<String>
    {
        return ResponseEntity(ex.message, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(AccountInactiveException::class)
    fun handleAccountInactive(ex: AccountInactiveException): ResponseEntity<String>
    {
        return ResponseEntity(ex.message, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleUnexpected(ex: RuntimeException): ResponseEntity<String>
    {
        return ResponseEntity("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
