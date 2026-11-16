package com.example.ticketengine.service

import com.example.ticketengine.exception.SeatNotAvailableException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SeatServiceConcurrencyTest {

    @Test
    fun `only one thread can hold the same seat under high contention`() {
        val seatService = SeatService()
        val threadCount = 50
        val executor = Executors.newFixedThreadPool(threadCount)

        val readyLatch = CountDownLatch(threadCount) // Waits for all 50 threads to initialize
        val startLatch = CountDownLatch(1)           // Starting gun that releases all threads together
        val doneLatch = CountDownLatch(threadCount)  // Waits for all 50 threads to finish

        val successCount = AtomicInteger(0)
        val conflictCount = AtomicInteger(0)

        try {
            for (i in 1..threadCount) {
                executor.submit {
                    try {
                        readyLatch.countDown()
                        startLatch.await() // All 50 threads pause right here until the gun fires

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

            readyLatch.await()                     // 1. Wait until all 50 threads are lined up
            startLatch.countDown()                 // 2. Fire the starting gun
            doneLatch.await(10, TimeUnit.SECONDS)  // 3. Wait for all 50 attempts to complete

            assertEquals(1, successCount.get(), "Exactly 1 user should successfully hold seat A1")
            assertEquals(threadCount - 1, conflictCount.get(), "The other 49 users should get SeatNotAvailableException")
        } finally {
            executor.shutdown()
        }
    }
}