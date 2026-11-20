package com.example.ticketengine.dto

import com.example.ticketengine.model.Seat
import com.example.ticketengine.model.Status

data class SeatResponse(
    val seatId: String,
    val row: String,
    val seatNumber: Int,
    val status: Status,
    val holdToken: String? = null,
)

fun Seat.toResponse(holdToken: String? = null): SeatResponse {

    val effectiveStatus = if (status == Status.HELD && isHoldExpired()) {
        Status.AVAILABLE
    } else {
        status
    }

    return SeatResponse(
        seatId = this.seatId,
        row = this.row,
        seatNumber = this.seatNumber,
        status = effectiveStatus,
        holdToken = holdToken,
    )
}
