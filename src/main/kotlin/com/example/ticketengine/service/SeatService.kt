package com.example.ticketengine.service

import com.example.ticketengine.dto.SeatResponse
import com.example.ticketengine.dto.toResponse
import com.example.ticketengine.exception.InvalidIdempotencyKeyException
import com.example.ticketengine.exception.SeatNotAvailableException
import com.example.ticketengine.exception.SeatNotFoundException
import com.example.ticketengine.exception.ShowNotFoundException
import com.example.ticketengine.exception.UnauthorizedHoldException
import com.example.ticketengine.model.IdempotencyRecord
import com.example.ticketengine.model.Seat
import com.example.ticketengine.model.Status
import com.example.ticketengine.repository.IdempotencyRecordRepository
import com.example.ticketengine.repository.SeatRepository
import com.example.ticketengine.repository.ShowRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class SeatService(
    private val seatRepository: SeatRepository,
    private val showRepository: ShowRepository,
    private val idempotencyRecordRepository: IdempotencyRecordRepository,
    private val objectMapper: ObjectMapper,
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
            val holdToken = UUID.randomUUID().toString()

            requestedSeat.heldByUserId = userId
            requestedSeat.status = Status.HELD
            requestedSeat.holdExpiresAt = Instant.now().plusSeconds(300L)
            requestedSeat.holdToken = holdToken

            return requestedSeat.toResponse(holdToken = holdToken)
        } else {
            throw SeatNotAvailableException("Seat ${requestedSeat.seatId} is not available")
        }
    }

    @Transactional
    fun confirmSeat(
        userId: String,
        showId: String,
        seatId: String,
        holdToken: String,
        idempotencyKey: String? = null
    ): SeatResponse {

        if (idempotencyKey != null) {
            val record = idempotencyRecordRepository.findById(idempotencyKey).orElse(null)
            if (record != null) {
                if (record.userId == userId && record.showId == showId && record.seatId == seatId) {
                    return objectMapper.readValue(record.responseBody, SeatResponse::class.java)
                }else {
                    throw InvalidIdempotencyKeyException("Idempotency key already used with different parameters")
                }
            }
        }
        val requestedSeat = getRequestedSeat(showId = showId, seatId = seatId)

        if (requestedSeat.status != Status.HELD || requestedSeat.isHoldExpired()) {
            if (requestedSeat.status == Status.HELD && requestedSeat.isHoldExpired()) {
                requestedSeat.status = Status.AVAILABLE
                requestedSeat.holdExpiresAt = null
                requestedSeat.heldByUserId = null
            }
            throw SeatNotAvailableException("Seat ${requestedSeat.seatId} is not available")
        }
        if (requestedSeat.heldByUserId != userId || requestedSeat.holdToken != holdToken) {
            throw UnauthorizedHoldException("Seat ${requestedSeat.seatId} is unauthorized")
        }
        requestedSeat.status = Status.OCCUPIED
        requestedSeat.heldByUserId = null
        requestedSeat.holdExpiresAt = null
        requestedSeat.holdToken = null

        val response = requestedSeat.toResponse()

        if(idempotencyKey != null) {
            idempotencyRecordRepository.save(
                IdempotencyRecord(
                    key = idempotencyKey,
                    userId = userId,
                    showId = showId,
                    seatId = seatId,
                    responseCode = 200,
                    responseBody = objectMapper.writeValueAsString(response)
                )
            )
        }

        return response
    }

    private fun getRequestedSeat(showId: String, seatId: String): Seat {

        if (!showRepository.existsById(showId)) throw ShowNotFoundException("Show not found")

        return seatRepository.findWithLockByShowIdAndSeatId(showId, seatId)
            ?: throw SeatNotFoundException("Seat with id $seatId not found")
    }
}