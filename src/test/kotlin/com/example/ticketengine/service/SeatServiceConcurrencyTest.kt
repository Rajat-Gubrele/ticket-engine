package com.example.ticketengine.service

import com.example.ticketengine.exception.SeatNotAvailableException
import com.example.ticketengine.model.Seat
import com.example.ticketengine.model.Status
import com.example.ticketengine.repository.SeatRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class SeatServiceConcurrencyTest {

    @Autowired
    private lateinit var seatService: SeatService

    @Autowired
    private lateinit var seatRepository: SeatRepository

    @BeforeEach
    fun resetSeatA1() {
        // Ensure Seat A1 is reset to AVAILABLE before the test runs
        val seat = seatRepository.findByShowIdAndSeatId("show-1", "A1")
        if (seat != null) {
            seat.status = Status.AVAILABLE
            seat.heldByUserId = null
            seat.holdExpiresAt = null
            seatRepository.save(seat)
        }
    }

    @Test
    fun `only one thread can hold the same seat under high contention`() {
        val threadCount = 50
        val executor = Executors.newFixedThreadPool(threadCount)

        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        val successCount = AtomicInteger(0)
        val conflictCount = AtomicInteger(0)

        try {
            for (i in 1..threadCount) {
                executor.submit {
                    try {
                        readyLatch.countDown()
                        startLatch.await()

                        seatService.holdSeat(
                            userId = "user-$i",
                            showId = "show-1",
                            seatId = "A1"
                        )
                        successCount.incrementAndGet()
                    } catch (e: SeatNotAvailableException) {
                        conflictCount.incrementAndGet()
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            readyLatch.await()
            startLatch.countDown()
            doneLatch.await(15, TimeUnit.SECONDS)

            assertEquals(1, successCount.get(), "Exactly 1 user should successfully hold seat A1")
            assertEquals(threadCount - 1, conflictCount.get(), "The other 49 users should get SeatNotAvailableException")
        } finally {
            executor.shutdown()
        }
    }
}