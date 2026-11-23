package com.example.ticketengine.controller

import com.example.ticketengine.model.Status
import com.example.ticketengine.repository.SeatRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class SeatControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var seatRepository: SeatRepository

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @BeforeEach
    fun setup() {

        redisTemplate.delete("shows:show-1:seats")

        // Reset B2 to AVAILABLE
        val seatB2 = seatRepository.findByShowIdAndSeatId("show-1", "B2")
        if (seatB2 != null) {
            seatB2.status = Status.AVAILABLE
            seatB2.heldByUserId = null
            seatB2.holdExpiresAt = null
            seatRepository.save(seatB2)
        }

        // Ensure B3 is HELD by user "124" with an active hold (expires in 300s)
        val seatB3 = seatRepository.findByShowIdAndSeatId("show-1", "B3")
        if (seatB3 != null) {
            seatB3.status = Status.HELD
            seatB3.heldByUserId = "124"
            seatB3.holdToken = "token-124"
            seatB3.holdExpiresAt = Instant.now().plusSeconds(300L)
            seatRepository.save(seatB3)
        }
    }

    @Test
    fun `GET seats returns 200 OK with all show seats`() {
        mockMvc.perform(get("/api/v1/shows/show-1/seats"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(6))
            .andExpect(jsonPath("$[0].seatId").exists())
            .andExpect(jsonPath("$[0].status").exists())
    }

    @Test
    fun `GET seats returns 404 NOT FOUND for non-existent show`() {
        mockMvc.perform(get("/api/v1/shows/unknown-show/seats"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Show not found"))
    }

    @Test
    fun `POST hold returns 201 CREATED when seat is available`() {
        mockMvc.perform(
            post("/api/v1/shows/show-1/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "userId": "user-100",
                        "seatId": "B2"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.seatId").value("B2"))
            .andExpect(jsonPath("$.status").value("HELD"))
    }

    @Test
    fun `POST hold returns 400 BAD REQUEST when userId is blank`() {
        mockMvc.perform(
            post("/api/v1/shows/show-1/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "userId": "",
                        "seatId": "B2"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `POST hold returns 409 CONFLICT when seat is already occupied`() {
        mockMvc.perform(
            post("/api/v1/shows/show-1/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "userId": "user-100",
                        "seatId": "A3"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Seat A3 is not available"))
    }

    @Test
    fun `POST confirm returns 403 FORBIDDEN when held by another user`() {
        mockMvc.perform(
            post("/api/v1/shows/show-1/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "userId": "hacker-user",
                        "seatId": "B3",
                        "holdToken": "token-124"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Seat B3 is unauthorized"))
    }

    @Test
    fun `POST confirm returns 403 FORBIDDEN when hold token is invalid`() {
        // Seat B3 is held by user "124", but we pass an incorrect holdToken
        mockMvc.perform(
            post("/api/v1/shows/show-1/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "userId": "124",
                        "seatId": "B3",
                        "holdToken": "wrong-token-999"
                    }
                """.trimIndent())
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Seat B3 is unauthorized"))
    }

    @Test
    fun `POST confirm returns 200 OK and transitions seat to OCCUPIED when user matches`() {
        // 1. Hold B2
        val holdResult = mockMvc.perform(
            post("/api/v1/shows/show-1/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId": "user-200", "seatId": "B2"}""")
        ).andExpect(status().isCreated)
            .andReturn()

// Extract the token using JsonPath or string parsing:
        val responseJson = com.jayway.jsonpath.JsonPath.parse(holdResult.response.contentAsString)
        val holdToken: String = responseJson.read("$.holdToken")

// 2. Confirm B2 with that token
        mockMvc.perform(
            post("/api/v1/shows/show-1/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {
                "userId": "user-200",
                "seatId": "B2",
                "holdToken": "$holdToken"
            }
        """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("OCCUPIED"))
    }
    @Test
    fun `POST confirm is idempotent - retrying with same Idempotency-Key returns 200 OK instead of 409 Conflict`() {
        // 1. Arrange: Hold B2 for user-300
        val holdResult = mockMvc.perform(
            post("/api/v1/shows/show-1/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId": "user-300", "seatId": "B2"}""")
        ).andExpect(status().isCreated).andReturn()

        val json = com.jayway.jsonpath.JsonPath.parse(holdResult.response.contentAsString)
        val holdToken: String = json.read("$.holdToken")
        val idempotencyKey = java.util.UUID.randomUUID().toString()

        val confirmBody = """
            {
                "userId": "user-300",
                "seatId": "B2",
                "holdToken": "$holdToken"
            }
        """.trimIndent()

        // 2. Act (Attempt 1): First confirm request with Idempotency-Key
        mockMvc.perform(
            post("/api/v1/shows/show-1/confirm")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(confirmBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seatId").value("B2"))
            .andExpect(jsonPath("$.status").value("OCCUPIED"))

        // 3. Act & Assert (Attempt 2 - Network Retry):
        // Re-sending the exact same request with the same Idempotency-Key MUST return 200 OK (replayed response),
        // not 409 Conflict!
        mockMvc.perform(
            post("/api/v1/shows/show-1/confirm")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(confirmBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seatId").value("B2"))
            .andExpect(jsonPath("$.status").value("OCCUPIED"))
    }

    @Test
    fun `POST confirm returns 400 BAD REQUEST when Idempotency-Key is reused with different parameters`() {
        // 1. Arrange: Hold B2 for user-400 and confirm it with a key
        val holdResult = mockMvc.perform(
            post("/api/v1/shows/show-1/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId": "user-400", "seatId": "B2"}""")
        ).andExpect(status().isCreated).andReturn()

        val json = com.jayway.jsonpath.JsonPath.parse(holdResult.response.contentAsString)
        val holdToken: String = json.read("$.holdToken")
        val idempotencyKey = java.util.UUID.randomUUID().toString()

        mockMvc.perform(
            post("/api/v1/shows/show-1/confirm")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId": "user-400", "seatId": "B2", "holdToken": "$holdToken"}""")
        ).andExpect(status().isOk)

        // 2. Act & Assert: Reusing the same key for a different seat (e.g. B3) must be rejected with 400
        mockMvc.perform(
            post("/api/v1/shows/show-1/confirm")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId": "user-400", "seatId": "B3", "holdToken": "token-124"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Idempotency key already used with different parameters"))
    }

    @Test
    fun `GET seats caches result in Redis and evicts on seat hold`() {
        val cacheKey = "shows:show-1:seats"

        // 1. Initially, cache is empty
        org.junit.jupiter.api.Assertions.assertFalse(redisTemplate.hasKey(cacheKey))

        // 2. Act: First GET triggers a Cache Miss and populates Redis
        mockMvc.perform(get("/api/v1/shows/show-1/seats"))
            .andExpect(status().isOk)

        // Assert: Redis now has the key cached
        org.junit.jupiter.api.Assertions.assertTrue(redisTemplate.hasKey(cacheKey))

        // 3. Act: Hold a seat (Write operation triggers Cache Eviction)
        mockMvc.perform(
            post("/api/v1/shows/show-1/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId": "cache-tester", "seatId": "B2"}""")
        ).andExpect(status().isCreated)

        // Assert: Cache key was evicted from Redis
        org.junit.jupiter.api.Assertions.assertFalse(redisTemplate.hasKey(cacheKey))
    }
}
