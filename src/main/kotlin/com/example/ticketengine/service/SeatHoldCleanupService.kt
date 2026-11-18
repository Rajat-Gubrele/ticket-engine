package com.example.ticketengine.service

import com.example.ticketengine.repository.SeatRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SeatHoldCleanupService(
    private val seatRepository: SeatRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger("SeatHoldCleanupService")
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    fun cleanupExpiredHolds() {
        val totalSeatsCleaned = seatRepository.releaseExpiredHolds(now = Instant.now())
        if(totalSeatsCleaned > 0) {
            logger.info("Released $totalSeatsCleaned expired seat holds back to AVAILABLE.")
        }
    }
}