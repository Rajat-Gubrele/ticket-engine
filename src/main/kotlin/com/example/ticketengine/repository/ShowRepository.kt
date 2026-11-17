package com.example.ticketengine.repository

import com.example.ticketengine.model.Seat
import com.example.ticketengine.model.Show
import org.springframework.data.jpa.repository.JpaRepository

interface ShowRepository : JpaRepository<Show, String> {}