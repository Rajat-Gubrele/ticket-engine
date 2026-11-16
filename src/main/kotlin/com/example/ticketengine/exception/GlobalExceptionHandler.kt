package com.example.ticketengine.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String
)

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ShowNotFoundException::class, SeatNotFoundException::class)
    fun handleNotFoundException(e: RuntimeException): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.message ?: "Resource not found")
    }

    @ExceptionHandler(SeatNotAvailableException::class)
    fun handleConflictException(e: SeatNotAvailableException): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(HttpStatus.CONFLICT, e.message ?: "Seat not available")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val validationMessage = e.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, validationMessage)
    }

    private fun buildErrorResponse(status: HttpStatus, message: String): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = message
        )
        return ResponseEntity.status(status).body(body)
    }
}