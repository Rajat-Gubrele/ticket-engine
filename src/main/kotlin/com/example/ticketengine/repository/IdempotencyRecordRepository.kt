package com.example.ticketengine.repository

import com.example.ticketengine.model.IdempotencyRecord
import org.springframework.data.jpa.repository.JpaRepository

interface IdempotencyRecordRepository : JpaRepository<IdempotencyRecord, String> {
}