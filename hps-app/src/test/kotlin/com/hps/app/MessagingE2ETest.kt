package com.hps.app

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class MessagingE2ETest : BaseE2ETest() {

    private lateinit var providerToken: String
    private lateinit var customerToken: String
    private lateinit var providerId: String
    private lateinit var customerId: String

    @BeforeEach
    fun setup() {
        val provEmail = "msg-prov-${System.nanoTime()}@test.com"
        val custEmail = "msg-cust-${System.nanoTime()}@test.com"

        api.register(provEmail, firstName = "MsgProv")
        api.register(custEmail, firstName = "MsgCust")

        val categories = api.json(api.get("/api/v1/categories"))
        val categoryId = categories[0]["id"].asText()

        api.post("/api/v1/providers/me", mapOf(
            "businessName" to "Msg Test Spa",
            "categoryIds" to listOf(categoryId)
        ), api.login(provEmail).accessToken)

        providerToken = api.login(provEmail).accessToken
        customerToken = api.login(custEmail).accessToken

        providerId = api.extractUserId(providerToken)
        customerId = api.extractUserId(customerToken)
    }

    @Test
    fun `customer can start conversation with provider`() {
        val resp = api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "Hi, I'd like to book!"
        ), customerToken)

        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val conv = api.json(resp)
        assertEquals("CUSTOMER_PROVIDER", conv["conversationType"].asText())
        assertEquals(providerId, conv["otherParticipant"]["id"].asText())
        assertNotNull(conv["lastMessage"])
    }

    @Test
    fun `provider can reply and messages are tracked`() {
        val convResp = api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "Hello!"
        ), customerToken)
        val convId = api.json(convResp)["id"].asText()

        // Provider replies
        val msgResp = api.post("/api/v1/conversations/$convId/messages", mapOf(
            "content" to "Hi there! How can I help?"
        ), providerToken)
        assertEquals(HttpStatus.CREATED, msgResp.statusCode)

        // Get messages
        val messages = api.json(api.get("/api/v1/conversations/$convId/messages", customerToken))
        assertEquals(2, messages.size())
    }

    @Test
    fun `unread count and mark read work`() {
        val convResp = api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "Test message"
        ), customerToken)
        val convId = api.json(convResp)["id"].asText()

        // Provider has 1 unread
        val unread = api.json(api.get("/api/v1/messages/unread-count", providerToken))
        assertTrue(unread["unread"].asLong() >= 1)

        // Provider marks read
        val readResp = api.post("/api/v1/conversations/$convId/read", null, providerToken)
        assertEquals(HttpStatus.OK, readResp.statusCode)

        // Unread should be 0 for this conversation
        val afterRead = api.json(api.get("/api/v1/messages/unread-count", providerToken))
        assertTrue(afterRead["unread"].asLong() < unread["unread"].asLong())
    }

    @Test
    fun `provider cannot message new customer without prior contact`() {
        val newCustEmail = "new-cust-${System.nanoTime()}@test.com"
        api.register(newCustEmail, firstName = "NewCust")
        val newCustId = api.extractUserId(api.login(newCustEmail).accessToken)

        val resp = api.post("/api/v1/conversations", mapOf(
            "participantId" to newCustId,
            "initialMessage" to "Hello!"
        ), providerToken)
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
    }

    @Test
    fun `provider can message customer after prior conversation`() {
        // Customer starts conversation
        api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "First contact"
        ), customerToken)

        // Now provider can initiate (already has contact)
        val resp = api.post("/api/v1/conversations", mapOf(
            "participantId" to customerId,
            "initialMessage" to "Following up!"
        ), providerToken)
        // Should reuse existing conversation
        assertEquals(HttpStatus.CREATED, resp.statusCode)
    }

    @Test
    fun `blocking prevents messaging`() {
        val convResp = api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "Before block"
        ), customerToken)
        val convId = api.json(convResp)["id"].asText()

        // Customer blocks provider
        api.post("/api/v1/users/blocks", mapOf("userId" to providerId), customerToken)

        // Provider cannot send message
        val msgResp = api.post("/api/v1/conversations/$convId/messages", mapOf(
            "content" to "After block"
        ), providerToken)
        assertEquals(HttpStatus.FORBIDDEN, msgResp.statusCode)

        // Customer unblocks
        api.delete("/api/v1/users/blocks/$providerId", customerToken)

        // Provider can send again
        val msgResp2 = api.post("/api/v1/conversations/$convId/messages", mapOf(
            "content" to "After unblock"
        ), providerToken)
        assertEquals(HttpStatus.CREATED, msgResp2.statusCode)
    }

    @Test
    fun `non-participant cannot access conversation`() {
        val convResp = api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "Private chat"
        ), customerToken)
        val convId = api.json(convResp)["id"].asText()

        val outsiderToken = api.register("outsider-${System.nanoTime()}@test.com").accessToken
        val resp = api.get("/api/v1/conversations/$convId/messages", outsiderToken)
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
    }

}
