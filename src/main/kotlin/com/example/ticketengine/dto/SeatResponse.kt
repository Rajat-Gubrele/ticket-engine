package com.example.ticketengine.dto

import com.example.ticketengine.model.Seat
import com.example.ticketengine.model.Status
import java.time.Instant

data class SeatResponse(
    val seatId: String,
    val row: String,
    val seatNumber: Int,
    val status: Status
)

fun Seat.toResponse(): SeatResponse {

    val effectiveStatus = if (status == Status.HELD && holdExpiresAt != null && Instant.now().isAfter(holdExpiresAt)) {
        Status.AVAILABLE
    } else {
        status
    }

    return SeatResponse(
        seatId = this.seatId,
        row = this.row,
        seatNumber = this.seatNumber,
        status = effectiveStatus
    )
}
