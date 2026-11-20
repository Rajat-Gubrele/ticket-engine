package com.example.ticketengine.repository

import com.example.ticketengine.model.Seat
import com.example.ticketengine.model.Status
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface SeatRepository: JpaRepository<Seat, Long> {

    fun findByShowId(showId: String): List<Seat>

    fun findByShowIdAndSeatId(showId: String, seatId: String): Seat?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockByShowIdAndSeatId(showId: String, seatId: String): Seat?

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Seat s
        set s.status = :availableStatus,
        s.heldByUserId = null,
        s.holdExpiresAt = null,
        s.holdToken = null
        where s.status = :heldStatus AND s.holdExpiresAt < :now
    """)
    fun releaseExpiredHolds(now: Instant, availableStatus: Status = Status.AVAILABLE, heldStatus: Status = Status.HELD): Int
}