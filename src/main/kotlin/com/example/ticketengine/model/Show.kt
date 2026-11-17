package com.example.ticketengine.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "shows")
class Show(
    @Id val id: String,
    @Column(nullable = false) val title: String
)