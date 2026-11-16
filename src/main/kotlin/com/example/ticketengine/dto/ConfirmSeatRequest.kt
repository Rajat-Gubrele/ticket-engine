package com.example.ticketengine.dto

import jakarta.validation.constraints.NotBlank

data class ConfirmSeatRequest(
    @field:NotBlank(message = "userId cannot be blank") val userId: String,
    @field:NotBlank(message = "seatId cannot be blank") val seatId: String,
)
