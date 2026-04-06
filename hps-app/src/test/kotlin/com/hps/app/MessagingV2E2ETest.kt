package com.hps.app

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

/**
 * Tests for messaging V2 features: handles, conversations/threads, archiving, reporting.
 */
class MessagingV2E2ETest : BaseE2ETest() {

    private lateinit var providerToken: String
    private lateinit var customerToken: String
    private lateinit var providerId: String
    private lateinit var customerId: String
    private lateinit var adminToken: String

    @BeforeEach
    fun setup() {
        // Login as super admin (seeded with default password, no API key headers)
        val adminLoginResp = api.post("/api/v1/auth/login", mapOf(
            "email" to "admin@hps.local",
            "password" to "changeme123"
        ), headers = emptyMap())
        adminToken = api.json(adminLoginResp)["accessToken"].asText()

        // Register provider with handle
        val provEmail = "prov-v2-${System.nanoTime()}@test.com"
        val provHandle = "prov${System.nanoTime() % 100000}"
        api.post("/api/v1/auth/register", mapOf(
            "email" to provEmail,
            "password" to "password123",
            "handle" to provHandle,
            "firstName" to "TestProv"
        ))

        // Make them a provider
        val categories = api.json(api.get("/api/v1/categories"))
        val categoryId = categories[0]["id"].asText()
        val provLogin = api.login(provEmail)
        api.post("/api/v1/providers/me", mapOf(
            "businessName" to "V2 Test Spa",
            "categoryIds" to listOf(categoryId)
        ), provLogin.accessToken)
        providerToken = api.login(provEmail).accessToken
        providerId = api.extractUserId(providerToken)

        // Register customer with handle
        val custEmail = "cust-v2-${System.nanoTime()}@test.com"
        val custHandle = "cust${System.nanoTime() % 100000}"
        api.post("/api/v1/auth/register", mapOf(
            "email" to custEmail,
            "password" to "password123",
            "handle" to custHandle,
            "firstName" to "TestCust"
        ))
        customerToken = api.login(custEmail).accessToken
        customerId = api.extractUserId(customerToken)
    }

    // === Handle Tests ===

    @Test
    fun `handle availability check - valid handle`() {
        val resp = api.get("/api/v1/auth/handle-available?handle=validhandle123")
        assertEquals(HttpStatus.OK, resp.statusCode)
        val json = api.json(resp)
        assertTrue(json["available"].asBoolean())
    }

    @Test
    fun `handle availability check - rejects admin in name`() {
        val resp = api.get("/api/v1/auth/handle-available?handle=myadmin")
        assertEquals(HttpStatus.OK, resp.statusCode)
        val json = api.json(resp)
        assertFalse(json["available"].asBoolean())
        assertTrue(json["reason"].asText().contains("admin"))
    }

    @Test
    fun `handle availability check - rejects invalid format`() {
        val resp = api.get("/api/v1/auth/handle-available?handle=AB")
        assertEquals(HttpStatus.OK, resp.statusCode)
        val json = api.json(resp)
        assertFalse(json["available"].asBoolean())
    }

    @Test
    fun `handle availability check - rejects taken handle`() {
        val handle = "taken${System.nanoTime() % 100000}"
        api.post("/api/v1/auth/register", mapOf(
            "email" to "taken-${System.nanoTime()}@test.com",
            "password" to "password123",
            "handle" to handle
        ))
        val resp = api.get("/api/v1/auth/handle-available?handle=$handle")
        val json = api.json(resp)
        assertFalse(json["available"].asBoolean())
    }

    @Test
    fun `registration with handle succeeds`() {
        val handle = "newuser${System.nanoTime() % 100000}"
        val resp = api.post("/api/v1/auth/register", mapOf(
            "email" to "handleuser-${System.nanoTime()}@test.com",
            "password" to "password123",
            "handle" to handle,
            "firstName" to "Handle"
        ))
        assertEquals(HttpStatus.CREATED, resp.statusCode)
    }

    @Test
    fun `registration with duplicate handle fails`() {
        val handle = "duphandle${System.nanoTime() % 100000}"
        api.post("/api/v1/auth/register", mapOf(
            "email" to "dup1-${System.nanoTime()}@test.com",
            "password" to "password123",
            "handle" to handle
        ))
        val resp = api.post("/api/v1/auth/register", mapOf(
            "email" to "dup2-${System.nanoTime()}@test.com",
            "password" to "password123",
            "handle" to handle
        ))
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }

    // === Conversation / Thread Tests ===

    @Test
    fun `customer can list their conversations`() {
        // Create a conversation first
        api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "Hello!"
        ), customerToken)

        val resp = api.get("/api/v1/conversations", customerToken)
        assertEquals(HttpStatus.OK, resp.statusCode)
        val convs = api.json(resp)
        assertTrue(convs.size() >= 1)

        // Verify conversation has handle in response
        val conv = convs[0]
        assertNotNull(conv["otherParticipant"]["handle"])
    }

    @Test
    fun `replies stay in the same conversation thread`() {
        // Customer starts conversation
        val createResp = api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "First message"
        ), customerToken)
        val convId = api.json(createResp)["id"].asText()

        // Provider replies
        api.post("/api/v1/conversations/$convId/messages", mapOf(
            "content" to "Provider reply 1"
        ), providerToken)

        // Customer replies again
        api.post("/api/v1/conversations/$convId/messages", mapOf(
            "content" to "Customer reply 2"
        ), customerToken)

        // Provider replies again
        api.post("/api/v1/conversations/$convId/messages", mapOf(
            "content" to "Provider reply 3"
        ), providerToken)

        // Get all messages - should be 4 in the same thread
        val messages = api.json(api.get("/api/v1/conversations/$convId/messages", customerToken))
        assertEquals(4, messages.size())

        // Verify handles are in message responses
        val firstMsg = messages[0]
        assertNotNull(firstMsg["senderHandle"])
    }

    @Test
    fun `starting another conversation with same user reuses existing thread`() {
        // Customer starts conversation
        val resp1 = api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "First conversation"
        ), customerToken)
        val convId1 = api.json(resp1)["id"].asText()

        // Customer starts another conversation with same provider
        val resp2 = api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "Second message"
        ), customerToken)
        val convId2 = api.json(resp2)["id"].asText()

        // Should be the same conversation
        assertEquals(convId1, convId2)

        // Should have 2 messages in the thread
        val messages = api.json(api.get("/api/v1/conversations/$convId1/messages", customerToken))
        assertEquals(2, messages.size())
    }

    @Test
    fun `can start conversation by handle`() {
        // Register a provider with known handle
        val handle = "byhandle${System.nanoTime() % 100000}"
        val email = "byhandle-${System.nanoTime()}@test.com"
        api.post("/api/v1/auth/register", mapOf(
            "email" to email,
            "password" to "password123",
            "handle" to handle,
            "firstName" to "ByHandle"
        ))
        val categories = api.json(api.get("/api/v1/categories"))
        val catId = categories[0]["id"].asText()
        val token = api.login(email).accessToken
        api.post("/api/v1/providers/me", mapOf(
            "businessName" to "Handle Spa",
            "categoryIds" to listOf(catId)
        ), token)

        // Customer starts conversation by handle
        val resp = api.post("/api/v1/conversations", mapOf(
            "participantHandle" to handle,
            "initialMessage" to "Hi @$handle!"
        ), customerToken)
        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val conv = api.json(resp)
        assertEquals(handle, conv["otherParticipant"]["handle"].asText())
    }

    // === Archive Tests ===

    @Test
    fun `customer can archive and unarchive conversation`() {
        val createResp = api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "Archive test"
        ), customerToken)
        val convId = api.json(createResp)["id"].asText()

        // Archive
        val archiveResp = api.post("/api/v1/conversations/$convId/archive", null, customerToken)
        assertEquals(HttpStatus.NO_CONTENT, archiveResp.statusCode)

        // Should not appear in normal list
        val normalList = api.json(api.get("/api/v1/conversations", customerToken))
        val normalIds = (0 until normalList.size()).map { normalList[it]["id"].asText() }
        assertFalse(normalIds.contains(convId))

        // Should appear in archived list
        val archivedList = api.json(api.get("/api/v1/conversations?archived=true", customerToken))
        val archivedIds = (0 until archivedList.size()).map { archivedList[it]["id"].asText() }
        assertTrue(archivedIds.contains(convId))

        // Unarchive
        val unarchiveResp = api.delete("/api/v1/conversations/$convId/archive", customerToken)
        assertEquals(HttpStatus.NO_CONTENT, unarchiveResp.statusCode)

        // Should be back in normal list
        val afterUnarchive = api.json(api.get("/api/v1/conversations", customerToken))
        val afterIds = (0 until afterUnarchive.size()).map { afterUnarchive[it]["id"].asText() }
        assertTrue(afterIds.contains(convId))
    }

    @Test
    fun `archiving is per-user - other participant still sees it`() {
        val createResp = api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "Per-user archive test"
        ), customerToken)
        val convId = api.json(createResp)["id"].asText()

        // Customer archives
        api.post("/api/v1/conversations/$convId/archive", null, customerToken)

        // Provider should still see it in their normal list
        val providerList = api.json(api.get("/api/v1/conversations", providerToken))
        val providerIds = (0 until providerList.size()).map { providerList[it]["id"].asText() }
        assertTrue(providerIds.contains(convId))
    }

    // === Report Tests ===

    @Test
    fun `user can report a message`() {
        val createResp = api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "Offensive content here"
        ), customerToken)
        val convId = api.json(createResp)["id"].asText()

        // Get messages to find the message ID
        val messages = api.json(api.get("/api/v1/conversations/$convId/messages", providerToken))
        val messageId = messages[0]["id"].asText()

        // Provider reports the message
        val reportResp = api.post("/api/v1/messages/$messageId/report", mapOf(
            "reason" to "Offensive language"
        ), providerToken)
        assertEquals(HttpStatus.CREATED, reportResp.statusCode)

        val report = api.json(reportResp)
        assertEquals("PENDING", report["status"].asText())
        assertEquals("Offensive language", report["reason"].asText())
    }

    @Test
    fun `cannot report own message`() {
        val createResp = api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "My own message"
        ), customerToken)
        val convId = api.json(createResp)["id"].asText()

        val messages = api.json(api.get("/api/v1/conversations/$convId/messages", customerToken))
        val messageId = messages[0]["id"].asText()

        val reportResp = api.post("/api/v1/messages/$messageId/report", mapOf(
            "reason" to "Trying to self-report"
        ), customerToken)
        assertEquals(HttpStatus.BAD_REQUEST, reportResp.statusCode)
    }

    @Test
    fun `cannot report same message twice`() {
        val createResp = api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "Report twice test"
        ), customerToken)
        val convId = api.json(createResp)["id"].asText()

        val messages = api.json(api.get("/api/v1/conversations/$convId/messages", providerToken))
        val messageId = messages[0]["id"].asText()

        // First report
        api.post("/api/v1/messages/$messageId/report", mapOf("reason" to "First"), providerToken)

        // Second report should fail
        val resp = api.post("/api/v1/messages/$messageId/report", mapOf("reason" to "Second"), providerToken)
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }

    @Test
    fun `admin can list and review reports`() {
        // Create a reportable message
        val createResp = api.post("/api/v1/conversations", mapOf(
            "participantId" to providerId,
            "initialMessage" to "Bad message for admin test"
        ), customerToken)
        val convId = api.json(createResp)["id"].asText()

        val messages = api.json(api.get("/api/v1/conversations/$convId/messages", providerToken))
        val messageId = messages[0]["id"].asText()

        // Report it
        api.post("/api/v1/messages/$messageId/report", mapOf("reason" to "Spam"), providerToken)

        // Admin lists reports (need X-Tenant-Id header for admin endpoints)
        val tenants = api.json(api.get("/api/v1/admin/tenants", adminToken, headers = emptyMap()))
        val tenantId = tenants[0]["id"].asText()

        val h = mapOf("X-Tenant-Id" to tenantId)
        val reportsResp = api.get("/api/v1/admin/message-reports", adminToken, headers = h)
        assertEquals(HttpStatus.OK, reportsResp.statusCode)
        val reports = api.json(reportsResp)
        assertTrue(reports.size() >= 1)

        // Review it
        val reportId = reports[reports.size() - 1]["id"].asText()
        val reviewResp = api.put("/api/v1/admin/message-reports/$reportId", mapOf(
            "status" to "REVIEWED",
            "adminNotes" to "Reviewed and noted"
        ), adminToken, headers = h)
        assertEquals(HttpStatus.OK, reviewResp.statusCode)
    }
}
