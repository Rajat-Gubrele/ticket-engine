package com.example.ticketengine.model

import java.time.Instant

data class Seat(
    val seatId: String,
    val row: String,
    val seatNumber: Int,
    var status: Status,
    var heldByUserId: String? = null,
    var holdExpiresAt: Instant? = null
){
    fun isHoldExpired() = holdExpiresAt != null && Instant.now().isAfter(holdExpiresAt)
}
