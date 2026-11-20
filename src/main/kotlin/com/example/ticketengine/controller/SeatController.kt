package com.example.ticketengine.controller

import com.example.ticketengine.dto.ConfirmSeatRequest
import com.example.ticketengine.dto.HoldSeatRequest
import com.example.ticketengine.dto.SeatResponse
import com.example.ticketengine.service.SeatService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/shows")
class SeatController(private val seatService: SeatService) {
    @GetMapping("/{show_id}/seats")
    fun getSeatAvailability(@PathVariable("show_id") showId: String) = seatService.getSeats(showId)

    @PostMapping("/{show_id}/hold")
    fun holdSeat(
        @PathVariable("show_id") showId: String,
        @Valid @RequestBody request: HoldSeatRequest
    ): ResponseEntity<SeatResponse> {
        val heldSeat = seatService.holdSeat(userId = request.userId, showId = showId, seatId = request.seatId)
        return ResponseEntity.status(HttpStatus.CREATED).body(heldSeat)
    }

    @PostMapping("/{show_id}/confirm")
    fun confirmSeat(
        @PathVariable("show_id") showId: String,
        @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?,
        @Valid @RequestBody request: ConfirmSeatRequest
    ): SeatResponse = seatService.confirmSeat(userId = request.userId, showId = showId, seatId = request.seatId, holdToken = request.holdToken, idempotencyKey = idempotencyKey)
}