package com.example.ticketengine.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(name = "seats", uniqueConstraints = [UniqueConstraint(columnNames = ["seat_id","show_id"])])
class Seat(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "show_id", nullable = false) val show: Show,
    @Column(name="seat_id", nullable = false) val seatId: String,
    @Column(name="seat_row", nullable = false) val row: String,
    @Column(name="seat_number", nullable = false) val seatNumber: Int,
    @Enumerated(EnumType.STRING) @Column(name="status", nullable = false) var status: Status,
    @Column(name="heldByUserId") var heldByUserId: String? = null,
    @Column(name="holdExpiresAt") var holdExpiresAt: Instant? = null,
    @Column(name = "hold_token") var holdToken: String? = null,
){
    fun isHoldExpired() = holdExpiresAt != null && Instant.now().isAfter(holdExpiresAt)
}