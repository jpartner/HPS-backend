package com.hps.app

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

/**
 * E2E tests for provider media upload, approval, and management.
 */
class ProviderMediaE2ETest : BaseE2ETest() {

    private lateinit var providerToken: String
    private lateinit var providerId: String
    private lateinit var adminToken: String

    private fun h() = mapOf(
        "X-Client-Id" to "11111111-1111-1111-1111-111111111111",
        "X-Client-Secret" to "hps-dev-secret-key"
    )
    private fun ah() = mapOf("X-Tenant-Id" to "00000000-0000-0000-0000-000000000001")

    @BeforeEach
    fun setup() {
        val adminResp = api.post("/api/v1/auth/login", mapOf(
            "email" to "admin@hps.local", "password" to "changeme123"
        ), headers = emptyMap())
        adminToken = api.json(adminResp)["accessToken"].asText()

        val email = "media-prov-${System.nanoTime()}@test.com"
        api.post("/api/v1/auth/register", mapOf(
            "email" to email, "password" to "password123",
            "handle" to "mediaprov${System.nanoTime() % 100000}", "firstName" to "MediaProv"
        ), headers = h())

        providerToken = api.login(email).accessToken
        val categories = api.json(api.get("/api/v1/categories", headers = h()))
        api.post("/api/v1/providers/me", mapOf(
            "businessName" to "Media Test Spa",
            "categoryIds" to listOf(categories[0]["id"].asText())
        ), providerToken, headers = h())

        providerId = api.extractUserId(providerToken)

        // Approve the provider
        api.put("/api/v1/admin/providers/$providerId/approve", mapOf(
            "approvalStatus" to "APPROVED"
        ), adminToken, headers = ah())
    }

    private fun addMediaByUrl(
        url: String = "https://example.com/photo.jpg",
        mediaType: String = "GALLERY",
        isPrivate: Boolean = false,
        blurRequested: Boolean = false
    ): String {
        val resp = api.post("/api/v1/providers/me/media/url", mapOf(
            "url" to url,
            "caption" to "Test media",
            "mediaType" to mediaType,
            "isPrivate" to isPrivate,
            "blurRequested" to blurRequested
        ), providerToken, headers = h())
        assertEquals(HttpStatus.CREATED, resp.statusCode) { "Add media failed: ${resp.body}" }
        return api.json(resp)["id"].asText()
    }

    @Test
    fun `uploaded media starts as PENDING`() {
        val mediaId = addMediaByUrl()
        val own = api.json(api.get("/api/v1/providers/me/media", providerToken, headers = h()))
        val item = own.first { it["id"].asText() == mediaId }
        assertEquals("PENDING", item["approvalStatus"].asText())
    }

    @Test
    fun `pending media not visible in public gallery`() {
        addMediaByUrl()
        val gallery = api.json(api.get("/api/v1/providers/$providerId/gallery", headers = h()))
        assertEquals(0, gallery.size())
    }

    @Test
    fun `admin can approve media`() {
        val mediaId = addMediaByUrl()

        val resp = api.put("/api/v1/admin/media/$mediaId/review", mapOf(
            "approvalStatus" to "APPROVED"
        ), adminToken, headers = ah())
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals("APPROVED", api.json(resp)["approvalStatus"].asText())
    }

    @Test
    fun `approved gallery media visible in public gallery`() {
        val mediaId = addMediaByUrl()
        api.put("/api/v1/admin/media/$mediaId/review", mapOf(
            "approvalStatus" to "APPROVED"
        ), adminToken, headers = ah())

        val gallery = api.json(api.get("/api/v1/providers/$providerId/gallery", headers = h()))
        assertTrue(gallery.size() >= 1)
        assertTrue(gallery.any { it["id"].asText() == mediaId })
    }

    @Test
    fun `admin can reject media with note`() {
        val mediaId = addMediaByUrl()

        val resp = api.put("/api/v1/admin/media/$mediaId/review", mapOf(
            "approvalStatus" to "REJECTED",
            "note" to "Image quality too low"
        ), adminToken, headers = ah())
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals("REJECTED", api.json(resp)["approvalStatus"].asText())
        assertEquals("Image quality too low", api.json(resp)["reviewNote"].asText())
    }

    @Test
    fun `rejected media not visible in public gallery`() {
        val mediaId = addMediaByUrl()
        api.put("/api/v1/admin/media/$mediaId/review", mapOf(
            "approvalStatus" to "REJECTED"
        ), adminToken, headers = ah())

        val gallery = api.json(api.get("/api/v1/providers/$providerId/gallery", headers = h()))
        assertFalse(gallery.any { it["id"].asText() == mediaId })
    }

    @Test
    fun `private media not visible in public gallery even when approved`() {
        val mediaId = addMediaByUrl(isPrivate = true)
        api.put("/api/v1/admin/media/$mediaId/review", mapOf(
            "approvalStatus" to "APPROVED"
        ), adminToken, headers = ah())

        val gallery = api.json(api.get("/api/v1/providers/$providerId/gallery", headers = h()))
        assertFalse(gallery.any { it["id"].asText() == mediaId })

        // But visible in provider's own list
        val own = api.json(api.get("/api/v1/providers/me/media", providerToken, headers = h()))
        assertTrue(own.any { it["id"].asText() == mediaId })
    }

    @Test
    fun `verification media is always private`() {
        val mediaId = addMediaByUrl(mediaType = "VERIFICATION", isPrivate = false)
        val own = api.json(api.get("/api/v1/providers/me/media", providerToken, headers = h()))
        val item = own.first { it["id"].asText() == mediaId }
        assertTrue(item["isPrivate"].asBoolean())
        assertEquals("VERIFICATION", item["mediaType"].asText())
    }

    @Test
    fun `blur request flag is stored`() {
        val mediaId = addMediaByUrl(blurRequested = true)
        val own = api.json(api.get("/api/v1/providers/me/media", providerToken, headers = h()))
        val item = own.first { it["id"].asText() == mediaId }
        assertTrue(item["blurRequested"].asBoolean())
    }

    @Test
    fun `provider can update media metadata`() {
        val mediaId = addMediaByUrl()

        val resp = api.put("/api/v1/providers/me/media/$mediaId", mapOf(
            "caption" to "Updated caption",
            "blurRequested" to true
        ), providerToken, headers = h())
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals("Updated caption", api.json(resp)["caption"].asText())
        assertTrue(api.json(resp)["blurRequested"].asBoolean())
    }

    @Test
    fun `provider can delete own media`() {
        val mediaId = addMediaByUrl()
        val delResp = api.delete("/api/v1/providers/me/media/$mediaId", providerToken, headers = h())
        assertEquals(HttpStatus.NO_CONTENT, delResp.statusCode)

        val own = api.json(api.get("/api/v1/providers/me/media", providerToken, headers = h()))
        assertFalse(own.any { it["id"].asText() == mediaId })
    }

    @Test
    fun `admin can list pending media for review`() {
        addMediaByUrl()
        addMediaByUrl(url = "https://example.com/photo2.jpg")

        val resp = api.get("/api/v1/admin/media?status=PENDING", adminToken, headers = ah())
        assertEquals(HttpStatus.OK, resp.statusCode)
        val items = api.json(resp)
        assertTrue(items.size() >= 2)
        items.forEach { assertEquals("PENDING", it["approvalStatus"].asText()) }
    }
}
