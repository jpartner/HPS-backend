package com.hps.app

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

/**
 * E2E tests for provider approval workflow.
 */
class ProviderApprovalE2ETest : BaseE2ETest() {

    private lateinit var adminToken: String

    // API key headers for the default tenant
    private fun h() = mapOf(
        "X-Client-Id" to "11111111-1111-1111-1111-111111111111",
        "X-Client-Secret" to "hps-dev-secret-key"
    )

    // Admin headers: tenant ID from the API key's tenant
    private fun ah() = mapOf("X-Tenant-Id" to "00000000-0000-0000-0000-000000000001")

    @BeforeEach
    fun setup() {
        val loginResp = api.post("/api/v1/auth/login", mapOf(
            "email" to "admin@hps.local",
            "password" to "changeme123"
        ), headers = emptyMap())
        assertEquals(HttpStatus.OK, loginResp.statusCode) { "Admin login failed: ${loginResp.body}" }
        adminToken = api.json(loginResp)["accessToken"].asText()
    }

    private fun registerProvider(): Pair<String, String> {
        val email = "prov-${System.nanoTime()}@test.com"
        val handle = "prov${System.nanoTime() % 100000}"
        api.post("/api/v1/auth/register", mapOf(
            "email" to email, "password" to "password123",
            "handle" to handle, "firstName" to "TestProv"
        ), headers = h())

        val token = api.login(email).accessToken
        val categories = api.json(api.get("/api/v1/categories", headers = h()))
        val categoryId = categories[0]["id"].asText()

        val createResp = api.post("/api/v1/providers/me", mapOf(
            "businessName" to "Test Spa $handle",
            "categoryIds" to listOf(categoryId)
        ), token, headers = h())
        assertEquals(HttpStatus.CREATED, createResp.statusCode) { "Create provider failed: ${createResp.body}" }

        return Pair(api.extractUserId(token), token)
    }

    @Test
    fun `new provider starts with PENDING_APPROVAL status`() {
        val (providerId, _) = registerProvider()

        val resp = api.get("/api/v1/admin/providers/$providerId", adminToken, headers = ah())
        assertEquals(HttpStatus.OK, resp.statusCode) { "Admin get provider failed: ${resp.body}" }
        val provider = api.json(resp)
        assertEquals("PENDING_APPROVAL", provider["approvalStatus"].asText())
        assertFalse(provider["isVerified"].asBoolean())
    }

    @Test
    fun `pending provider is not visible in public detail`() {
        val (providerId, _) = registerProvider()
        val resp = api.get("/api/v1/providers/$providerId", headers = h())
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }

    @Test
    fun `admin can approve provider`() {
        val (providerId, _) = registerProvider()

        val resp = api.put("/api/v1/admin/providers/$providerId/approve", mapOf(
            "approvalStatus" to "APPROVED"
        ), adminToken, headers = ah())
        assertEquals(HttpStatus.OK, resp.statusCode) { "Approve failed: ${resp.body}" }

        val provider = api.json(resp)
        assertEquals("APPROVED", provider["approvalStatus"].asText())
        assertTrue(provider["isVerified"].asBoolean())
    }

    @Test
    fun `approved provider is visible in public detail`() {
        val (providerId, _) = registerProvider()

        api.put("/api/v1/admin/providers/$providerId/approve", mapOf(
            "approvalStatus" to "APPROVED"
        ), adminToken, headers = ah())

        val resp = api.get("/api/v1/providers/$providerId", headers = h())
        assertEquals(HttpStatus.OK, resp.statusCode)
    }

    @Test
    fun `admin can reject provider`() {
        val (providerId, _) = registerProvider()

        val resp = api.put("/api/v1/admin/providers/$providerId/approve", mapOf(
            "approvalStatus" to "REJECTED"
        ), adminToken, headers = ah())
        assertEquals(HttpStatus.OK, resp.statusCode) { "Reject failed: ${resp.body}" }

        val provider = api.json(resp)
        assertEquals("REJECTED", provider["approvalStatus"].asText())
        assertFalse(provider["isVerified"].asBoolean())
    }

    @Test
    fun `rejected provider is not visible in public detail`() {
        val (providerId, _) = registerProvider()

        api.put("/api/v1/admin/providers/$providerId/approve", mapOf(
            "approvalStatus" to "REJECTED"
        ), adminToken, headers = ah())

        val resp = api.get("/api/v1/providers/$providerId", headers = h())
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }

    @Test
    fun `admin can filter providers by approval status`() {
        registerProvider()

        val resp = api.get("/api/v1/admin/providers?approvalStatus=PENDING_APPROVAL", adminToken, headers = ah())
        assertEquals(HttpStatus.OK, resp.statusCode)
        val providers = api.json(resp)
        assertTrue(providers.size() >= 1)
        for (i in 0 until providers.size()) {
            assertEquals("PENDING_APPROVAL", providers[i]["approvalStatus"].asText())
        }
    }

    @Test
    fun `admin can revoke approval`() {
        val (providerId, _) = registerProvider()

        api.put("/api/v1/admin/providers/$providerId/approve", mapOf(
            "approvalStatus" to "APPROVED"
        ), adminToken, headers = ah())

        assertEquals(HttpStatus.OK, api.get("/api/v1/providers/$providerId", headers = h()).statusCode)

        api.put("/api/v1/admin/providers/$providerId/approve", mapOf(
            "approvalStatus" to "REJECTED"
        ), adminToken, headers = ah())

        assertEquals(HttpStatus.NOT_FOUND, api.get("/api/v1/providers/$providerId", headers = h()).statusCode)
    }

    @Test
    fun `provider handle is included in admin response`() {
        val (providerId, _) = registerProvider()

        val resp = api.get("/api/v1/admin/providers/$providerId", adminToken, headers = ah())
        assertEquals(HttpStatus.OK, resp.statusCode)
        val provider = api.json(resp)
        assertNotNull(provider["handle"])
        assertFalse(provider["handle"].isNull)
    }

    @Test
    fun `admin can request changes with notes`() {
        val (providerId, _) = registerProvider()

        val resp = api.put("/api/v1/admin/providers/$providerId/approve", mapOf(
            "approvalStatus" to "CHANGES_REQUESTED",
            "notes" to "Please add a profile photo and more detailed description"
        ), adminToken, headers = ah())
        assertEquals(HttpStatus.OK, resp.statusCode)

        val provider = api.json(resp)
        assertEquals("CHANGES_REQUESTED", provider["approvalStatus"].asText())
        assertEquals("Please add a profile photo and more detailed description", provider["approvalNotes"].asText())
        assertFalse(provider["isVerified"].asBoolean())
    }

    @Test
    fun `provider update after changes requested resets to pending`() {
        val (providerId, providerToken) = registerProvider()

        // Admin requests changes
        api.put("/api/v1/admin/providers/$providerId/approve", mapOf(
            "approvalStatus" to "CHANGES_REQUESTED",
            "notes" to "Need more info"
        ), adminToken, headers = ah())

        // Provider updates their profile
        api.put("/api/v1/providers/me", mapOf(
            "description" to "Updated description with more details"
        ), providerToken, headers = h())

        // Should be back to PENDING_APPROVAL
        val resp = api.get("/api/v1/admin/providers/$providerId", adminToken, headers = ah())
        assertEquals(HttpStatus.OK, resp.statusCode)
        val provider = api.json(resp)
        assertEquals("PENDING_APPROVAL", provider["approvalStatus"].asText())
        assertTrue(provider["approvalNotes"].isNull)
    }

    @Test
    fun `provider update after rejection resets to pending`() {
        val (providerId, providerToken) = registerProvider()

        // Admin rejects
        api.put("/api/v1/admin/providers/$providerId/approve", mapOf(
            "approvalStatus" to "REJECTED",
            "notes" to "Inappropriate content"
        ), adminToken, headers = ah())

        // Provider updates profile
        api.put("/api/v1/providers/me", mapOf(
            "businessName" to "Cleaned Up Business Name"
        ), providerToken, headers = h())

        // Should be back to PENDING_APPROVAL
        val resp = api.get("/api/v1/admin/providers/$providerId", adminToken, headers = ah())
        val provider = api.json(resp)
        assertEquals("PENDING_APPROVAL", provider["approvalStatus"].asText())
    }

    @Test
    fun `approval history tracks all status changes`() {
        val (providerId, providerToken) = registerProvider()

        // Admin requests changes
        api.put("/api/v1/admin/providers/$providerId/approve", mapOf(
            "approvalStatus" to "CHANGES_REQUESTED",
            "notes" to "Add a photo"
        ), adminToken, headers = ah())

        // Provider resubmits
        api.put("/api/v1/providers/me", mapOf(
            "description" to "Updated"
        ), providerToken, headers = h())

        // Admin approves
        api.put("/api/v1/admin/providers/$providerId/approve", mapOf(
            "approvalStatus" to "APPROVED",
            "notes" to "Looks good now"
        ), adminToken, headers = ah())

        // Check history
        val histResp = api.get("/api/v1/admin/providers/$providerId/history", adminToken, headers = ah())
        assertEquals(HttpStatus.OK, histResp.statusCode)
        val history = api.json(histResp)

        // Should have 3 entries (newest first): APPROVED, PENDING_APPROVAL (resubmit), CHANGES_REQUESTED
        assertTrue(history.size() >= 3) { "Expected at least 3 history entries, got ${history.size()}" }
        assertEquals("APPROVED", history[0]["status"].asText())
        assertEquals("Looks good now", history[0]["notes"].asText())
        assertEquals("PENDING_APPROVAL", history[1]["status"].asText())
        assertEquals("CHANGES_REQUESTED", history[2]["status"].asText())
        assertEquals("Add a photo", history[2]["notes"].asText())

        // Each entry has a changedByEmail
        assertFalse(history[0]["changedByEmail"].isNull)
    }
}
