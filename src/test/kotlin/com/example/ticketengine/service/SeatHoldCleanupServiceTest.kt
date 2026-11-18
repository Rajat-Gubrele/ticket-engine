package com.example.ticketengine.service

import com.example.ticketengine.model.Status
import com.example.ticketengine.repository.SeatRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant

@SpringBootTest
class SeatHoldCleanupServiceTest {

    @Autowired
    private lateinit var cleanupService: SeatHoldCleanupService

    @Autowired
    private lateinit var seatRepository: SeatRepository

    @Test
    fun `cleanupExpiredHolds should reset expired seats to AVAILABLE`() {
        // 1. Arrange: Ensure an expired seat exists
        val seat = seatRepository.findByShowIdAndSeatId("show-1", "A2")!!
        seat.status = Status.HELD
        seat.heldByUserId = "user-expire-test"
        seat.holdExpiresAt = Instant.now().minusSeconds(60)
        seatRepository.save(seat)

        // 2. Act: Trigger the sweeper
        cleanupService.cleanupExpiredHolds()

        // 3. Assert: Verify the seat was released
        val updatedSeat = seatRepository.findByShowIdAndSeatId("show-1", "A2")!!
        assertEquals(Status.AVAILABLE, updatedSeat.status)
        assertNull(updatedSeat.heldByUserId)
        assertNull(updatedSeat.holdExpiresAt)
    }
}