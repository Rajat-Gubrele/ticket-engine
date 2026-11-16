package com.example.ticketengine.service

import com.example.ticketengine.dto.SeatResponse
import com.example.ticketengine.dto.toResponse
import com.example.ticketengine.exception.SeatNotAvailableException
import com.example.ticketengine.exception.SeatNotFoundException
import com.example.ticketengine.exception.ShowNotFoundException
import com.example.ticketengine.exception.UnauthorizedHoldException
import com.example.ticketengine.model.Seat
import com.example.ticketengine.model.Status
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class SeatService {
    private val showSeats = ConcurrentHashMap<String, List<Seat>>()

    init {
        showSeats["show-1"] = listOf(
            Seat(seatId = "A1", row = "A", seatNumber = 1, status = Status.AVAILABLE),
            Seat(
                seatId = "A2",
                row = "A",
                seatNumber = 2,
                status = Status.HELD,
                heldByUserId = "123",
                holdExpiresAt = Instant.now()
            ),
            Seat(seatId = "A3", row = "A", seatNumber = 3, status = Status.OCCUPIED),
            Seat(seatId = "B1", row = "B", seatNumber = 1, status = Status.OCCUPIED),
            Seat(seatId = "B2", row = "B", seatNumber = 2, status = Status.AVAILABLE),
            Seat(
                seatId = "B3",
                row = "B",
                seatNumber = 3,
                status = Status.HELD,
                heldByUserId = "124",
                holdExpiresAt = Instant.now().plusSeconds(300L)
            ),
        )
    }

    fun getSeats(showId: String): List<SeatResponse> {
        return showSeats[showId]?.map { it.toResponse() } ?: throw ShowNotFoundException("Show not found")
    }

    fun holdSeat(userId: String, showId: String, seatId: String): SeatResponse {

        val requestedSeat = getRequestedSeat(showId = showId, seatId = seatId)

        synchronized(requestedSeat) {
            if (requestedSeat.status == Status.AVAILABLE || (requestedSeat.status == Status.HELD && requestedSeat.isHoldExpired())
            ) {
                requestedSeat.heldByUserId = userId
                requestedSeat.status = Status.HELD
                requestedSeat.holdExpiresAt = Instant.now().plusSeconds(300L)

                return requestedSeat.toResponse()
            } else {
                throw SeatNotAvailableException("Seat ${requestedSeat.seatId} is not available")
            }
        }
    }

    fun confirmSeat(userId: String, showId: String, seatId: String): SeatResponse {
        val requestedSeat = getRequestedSeat(showId = showId, seatId = seatId)
        synchronized(requestedSeat) {
            if (requestedSeat.status != Status.HELD || requestedSeat.isHoldExpired()) {
                if (requestedSeat.status == Status.HELD && requestedSeat.isHoldExpired()) {
                    requestedSeat.status = Status.AVAILABLE
                    requestedSeat.holdExpiresAt = null
                    requestedSeat.heldByUserId = null
                }
                throw SeatNotAvailableException("Seat ${requestedSeat.seatId} is not available")
            }
            if (requestedSeat.heldByUserId != userId) {
                throw UnauthorizedHoldException("Seat ${requestedSeat.seatId} is unauthorized")
            }
            requestedSeat.status = Status.OCCUPIED
            requestedSeat.heldByUserId = null
            requestedSeat.holdExpiresAt = null

            return requestedSeat.toResponse()
        }
    }

    private fun getRequestedSeat(showId: String, seatId: String): Seat {
        if (!showSeats.containsKey(showId)) {
            throw ShowNotFoundException("Show with id $showId not found")
        }
        return showSeats[showId]?.find { it.seatId == seatId }
            ?: throw SeatNotFoundException("Seat with id $seatId not found")
    }
}