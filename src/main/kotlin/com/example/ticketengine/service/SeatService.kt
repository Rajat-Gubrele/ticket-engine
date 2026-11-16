package com.example.ticketengine.service

import com.example.ticketengine.dto.SeatResponse
import com.example.ticketengine.dto.toResponse
import com.example.ticketengine.exception.SeatNotAvailableException
import com.example.ticketengine.exception.SeatNotFoundException
import com.example.ticketengine.exception.ShowNotFoundException
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
        return showSeats[showId]?.map { it.toResponse() } ?: emptyList()
    }

    fun holdSeat(userId: String, showId: String, seatId: String): SeatResponse = synchronized(seatId) {
        if (!showSeats.containsKey(showId)) {
            throw ShowNotFoundException("Show with id $showId not found")
        }
        val requestedSeat =
            showSeats[showId]?.filter { it.seatId == seatId }?.getOrNull(0) ?: throw SeatNotFoundException("Seat with id $seatId not found")

        if (requestedSeat.status == Status.AVAILABLE || (requestedSeat.status == Status.HELD && Instant.now()
                .isAfter(requestedSeat.holdExpiresAt))
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