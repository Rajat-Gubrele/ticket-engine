package com.example.ticketengine.repository

import com.example.ticketengine.model.Seat
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface SeatRepository: JpaRepository<Seat, Long> {

    fun findByShowId(showId: String): List<Seat>

    fun findByShowIdAndSeatId(showId: String, seatId: String): Seat?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockByShowIdAndSeatId(showId: String, seatId: String): Seat?
}