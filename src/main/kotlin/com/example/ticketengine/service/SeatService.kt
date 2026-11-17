package com.example.ticketengine.service

import com.example.ticketengine.dto.SeatResponse
import com.example.ticketengine.dto.toResponse
import com.example.ticketengine.exception.SeatNotAvailableException
import com.example.ticketengine.exception.SeatNotFoundException
import com.example.ticketengine.exception.ShowNotFoundException
import com.example.ticketengine.exception.UnauthorizedHoldException
import com.example.ticketengine.model.Seat
import com.example.ticketengine.model.Status
import com.example.ticketengine.repository.SeatRepository
import com.example.ticketengine.repository.ShowRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class SeatService(
    private val seatRepository: SeatRepository,
    private val showRepository: ShowRepository,
) {

    @Transactional(readOnly = true)
    fun getSeats(showId: String): List<SeatResponse> {
        if (!showRepository.existsById(showId)) throw ShowNotFoundException("Show not found")
        return seatRepository.findByShowId(showId).map { it.toResponse() }
    }

    @Transactional
    fun holdSeat(userId: String, showId: String, seatId: String): SeatResponse {

        val requestedSeat = getRequestedSeat(showId = showId, seatId = seatId)

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
    @Transactional
    fun confirmSeat(userId: String, showId: String, seatId: String): SeatResponse {

        val requestedSeat = getRequestedSeat(showId = showId, seatId = seatId)

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

    private fun getRequestedSeat(showId: String, seatId: String): Seat {

        if (!showRepository.existsById(showId)) throw ShowNotFoundException("Show not found")

        return seatRepository.findWithLockByShowIdAndSeatId(showId, seatId)
            ?: throw SeatNotFoundException("Seat with id $seatId not found")
    }
}