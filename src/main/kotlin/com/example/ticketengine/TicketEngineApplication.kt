package com.example.ticketengine

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class TicketEngineApplication

fun main(args: Array<String>) {
    runApplication<TicketEngineApplication>(*args)
}
