package com.example.ticketengine.dto

import jakarta.validation.constraints.NotBlank

data class HoldSeatRequest(
    @field:NotBlank val userId: String,
    @field:NotBlank val seatId: String,
)
