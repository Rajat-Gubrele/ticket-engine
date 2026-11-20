package com.example.ticketengine.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "idempotency_records")
class IdempotencyRecord(
    @Id
    val key: String,

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(name = "show_id", nullable = false)
    val showId: String,

    @Column(name = "seat_id", nullable = false)
    val seatId: String,

    @Column(name = "response_code", nullable = false)
    val responseCode: Int,

    @Column(name = "response_body", columnDefinition = "TEXT", nullable = false)
    val responseBody: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)