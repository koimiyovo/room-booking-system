package com.kyovo.infrastructure.api.controller

import com.kyovo.domain.exception.AccountNotOwnedByUserException
import com.kyovo.domain.exception.BookingAlreadyCancelledException
import com.kyovo.domain.exception.BookingConflictException
import com.kyovo.domain.exception.BookingNotFoundException
import com.kyovo.domain.exception.BookingNotOwnedByUserException
import com.kyovo.domain.exception.EmailAlreadyUsedException
import com.kyovo.domain.exception.InvalidCredentialsException
import com.kyovo.domain.exception.RoomCapacityExceededException
import com.kyovo.domain.exception.RoomNotFoundException
import com.kyovo.domain.exception.UserAlreadyActiveException
import com.kyovo.domain.exception.UserNotFoundException
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

    @ExceptionHandler(UserAlreadyActiveException::class)
    fun handleUserAlreadyActive(ex: UserAlreadyActiveException): ResponseEntity<String>
    {
        return ResponseEntity(ex.message, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleUnexpected(ex: RuntimeException): ResponseEntity<String>
    {
        return ResponseEntity("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
