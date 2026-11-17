package com.example.ticketengine.config

import com.example.ticketengine.model.Seat
import com.example.ticketengine.model.Show
import com.example.ticketengine.model.Status
import com.example.ticketengine.repository.SeatRepository
import com.example.ticketengine.repository.ShowRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class DataInitializer(
    private val showRepository: ShowRepository,
    private val seatRepository: SeatRepository
): CommandLineRunner {
    override fun run(vararg args: String) {
        if(!showRepository.existsById("show-1")) {
            val show = showRepository.save(Show(id = "show-1", title = "Hamilton"))

            val baselineSeats = listOf(
                Seat(
                    show = show,
                    seatId = "A1",
                    row = "A",
                    seatNumber = 1,
                    status = Status.AVAILABLE
                ),
                Seat(
                    show = show,
                    seatId = "A2",
                    row = "A",
                    seatNumber = 2,
                    status = Status.HELD,
                    heldByUserId = "123",
                    holdExpiresAt = Instant.now() // Expired hold
                ),
                Seat(
                    show = show,
                    seatId = "A3",
                    row = "A",
                    seatNumber = 3,
                    status = Status.OCCUPIED
                ),
                Seat(
                    show = show,
                    seatId = "B1",
                    row = "B",
                    seatNumber = 1,
                    status = Status.OCCUPIED
                ),
                Seat(
                    show = show,
                    seatId = "B2",
                    row = "B",
                    seatNumber = 2,
                    status = Status.AVAILABLE
                ),
                Seat(
                    show = show,
                    seatId = "B3",
                    row = "B",
                    seatNumber = 3,
                    status = Status.HELD,
                    heldByUserId = "124",
                    holdExpiresAt = Instant.now().plusSeconds(300L) // Active hold
                )
            )

            seatRepository.saveAll(baselineSeats)
        }
    }
}