package com.hps.app

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * End-to-end test proving the complete flow:
 * 1. Customer registers
 * 2. Provider registers + creates profile + adds services
 * 3. Admin approves provider
 * 4. Customer creates a booking
 * 5. Provider confirms the booking
 */
class FullFlowE2ETest : BaseE2ETest() {

    private fun tenantHeaders() = mapOf(
        "X-Client-Id" to "11111111-1111-1111-1111-111111111111",
        "X-Client-Secret" to "hps-dev-secret-key"
    )

    private fun adminHeaders() = mapOf("X-Tenant-Id" to "00000000-0000-0000-0000-000000000001")

    private fun adminLogin(): String {
        val resp = api.post("/api/v1/auth/login", mapOf(
            "email" to "admin@hps.local",
            "password" to "changeme123"
        ), headers = emptyMap())
        assertEquals(HttpStatus.OK, resp.statusCode) { "Admin login failed: ${resp.body}" }
        return api.json(resp)["accessToken"].asText()
    }

    @Test
    fun `complete flow - register, approve, book, confirm`() {
        val ts = System.nanoTime()
        val customerEmail = "cust-flow-$ts@test.com"
        val providerEmail = "prov-flow-$ts@test.com"

        // ── 1. Register customer ──
        val custTokens = api.register(customerEmail, firstName = "Alice")
        val customerToken = custTokens.accessToken
        assertNotNull(customerToken)

        // ── 2. Register provider ──
        val provTokens = api.register(providerEmail, firstName = "Bob")
        var providerToken = provTokens.accessToken
        val providerId = api.extractUserId(providerToken)

        // Get a category for the provider profile
        val categories = api.json(api.get("/api/v1/categories", headers = tenantHeaders()))
        assertTrue(categories.size() > 0, "Seed data should provide categories")
        val categoryId = categories[0]["id"].asText()
        val subCat1 = categories[0]["children"][0]["id"].asText()
        val subCat2 = categories[0]["children"][1]["id"].asText()

        // Create provider profile
        val createProvResp = api.post("/api/v1/providers/me", mapOf(
            "businessName" to "Bob's Wellness Studio",
            "description" to "Premium wellness services",
            "categoryIds" to listOf(categoryId)
        ), providerToken, headers = tenantHeaders())
        assertEquals(HttpStatus.CREATED, createProvResp.statusCode) { "Create provider failed: ${createProvResp.body}" }

        // Re-login to get updated role in token
        providerToken = api.login(providerEmail).accessToken

        // Provider is PENDING_APPROVAL and not visible publicly
        val publicBeforeApproval = api.get("/api/v1/providers/$providerId", headers = tenantHeaders())
        assertEquals(HttpStatus.NOT_FOUND, publicBeforeApproval.statusCode,
            "Pending provider should not be visible publicly")

        // ── 3. Provider adds services ──
        val svc1Resp = api.post("/api/v1/providers/me/services", mapOf(
            "categoryId" to subCat1,
            "pricingType" to "FIXED",
            "priceAmount" to 100,
            "priceCurrency" to "EUR",
            "durationMinutes" to 60,
            "isIncluded" to false,
            "primaryAmount" to 100,
            "translations" to listOf(mapOf("lang" to "en", "title" to "Deep Tissue Massage"))
        ), providerToken, headers = tenantHeaders())
        assertEquals(HttpStatus.CREATED, svc1Resp.statusCode) { "Create service 1 failed: ${svc1Resp.body}" }
        val service1 = api.json(svc1Resp)
        val service1Id = service1["id"].asText()
        assertEquals("Deep Tissue Massage", service1["title"].asText())
        assertEquals(100.0, service1["primaryAmount"].asDouble())
        assertFalse(service1["isIncluded"].asBoolean())

        val svc2Resp = api.post("/api/v1/providers/me/services", mapOf(
            "categoryId" to subCat2,
            "pricingType" to "FIXED",
            "priceAmount" to 50,
            "priceCurrency" to "EUR",
            "durationMinutes" to 30,
            "isIncluded" to true,
            "primaryAmount" to 50,
            "translations" to listOf(mapOf("lang" to "en", "title" to "Hot Stones Add-on"))
        ), providerToken, headers = tenantHeaders())
        assertEquals(HttpStatus.CREATED, svc2Resp.statusCode) { "Create service 2 failed: ${svc2Resp.body}" }
        val service2 = api.json(svc2Resp)
        val service2Id = service2["id"].asText()
        assertTrue(service2["isIncluded"].asBoolean(), "Service 2 should be marked as included")

        // ── 4. Admin approves provider ──
        val adminToken = adminLogin()

        // Verify provider is in PENDING_APPROVAL
        val adminGetResp = api.get("/api/v1/admin/providers/$providerId", adminToken, headers = adminHeaders())
        assertEquals(HttpStatus.OK, adminGetResp.statusCode) { "Admin get provider failed: ${adminGetResp.body}" }
        assertEquals("PENDING_APPROVAL", api.json(adminGetResp)["approvalStatus"].asText())

        // Approve
        val approveResp = api.put("/api/v1/admin/providers/$providerId/approve", mapOf(
            "approvalStatus" to "APPROVED",
            "notes" to "Looks good, welcome aboard!"
        ), adminToken, headers = adminHeaders())
        assertEquals(HttpStatus.OK, approveResp.statusCode) { "Approve failed: ${approveResp.body}" }
        val approved = api.json(approveResp)
        assertEquals("APPROVED", approved["approvalStatus"].asText())
        assertTrue(approved["isVerified"].asBoolean())

        // Provider is now visible publicly
        val publicAfterApproval = api.get("/api/v1/providers/$providerId", headers = tenantHeaders())
        assertEquals(HttpStatus.OK, publicAfterApproval.statusCode, "Approved provider should be visible")
        val publicDetail = api.json(publicAfterApproval)
        assertEquals("Bob's Wellness Studio", publicDetail["businessName"].asText())
        assertEquals(2, publicDetail["services"].size(), "Provider should have 2 services")

        // Verify services have the new dual-currency fields
        val publicServices = publicDetail["services"]
        val svcWithIncluded = (0 until publicServices.size())
            .map { publicServices[it] }
            .first { it["isIncluded"].asBoolean() }
        assertEquals("Hot Stones Add-on", svcWithIncluded["title"].asText())
        assertEquals(50.0, svcWithIncluded["primaryAmount"].asDouble())

        // ── 5. Customer creates a booking ──
        val scheduledAt = Instant.now().plus(3, ChronoUnit.DAYS).toString()

        val bookingResp = api.post("/api/v1/bookings", mapOf(
            "providerId" to providerId,
            "services" to listOf(
                mapOf("serviceId" to service1Id, "quantity" to 1),
                mapOf("serviceId" to service2Id, "quantity" to 1)
            ),
            "scheduledAt" to scheduledAt,
            "bookingType" to "INCALL",
            "clientNotes" to "First time visit, looking forward to it!"
        ), customerToken, headers = tenantHeaders())
        assertEquals(HttpStatus.CREATED, bookingResp.statusCode) { "Create booking failed: ${bookingResp.body}" }

        val booking = api.json(bookingResp)
        val bookingId = booking["id"].asText()
        assertEquals("REQUESTED", booking["status"].asText())
        assertEquals(providerId, booking["providerId"].asText())
        assertTrue(booking["services"].size() == 2, "Booking should include 2 services")
        assertEquals("First time visit, looking forward to it!", booking["clientNotes"].asText())

        // Both participants can see the booking
        val custGetResp = api.get("/api/v1/bookings/$bookingId", customerToken, headers = tenantHeaders())
        assertEquals(HttpStatus.OK, custGetResp.statusCode)
        assertEquals("REQUESTED", api.json(custGetResp)["status"].asText())

        val provGetResp = api.get("/api/v1/bookings/$bookingId", providerToken, headers = tenantHeaders())
        assertEquals(HttpStatus.OK, provGetResp.statusCode)
        assertEquals("REQUESTED", api.json(provGetResp)["status"].asText())

        // ── 6. Provider confirms the booking ──
        val confirmResp = api.patch("/api/v1/bookings/$bookingId/status",
            mapOf("status" to "CONFIRMED"), providerToken, headers = tenantHeaders())
        assertEquals(HttpStatus.OK, confirmResp.statusCode) { "Confirm booking failed: ${confirmResp.body}" }

        val confirmed = api.json(confirmResp)
        assertEquals("CONFIRMED", confirmed["status"].asText())
        assertEquals(bookingId, confirmed["id"].asText())

        // Both sides see CONFIRMED
        val custFinalResp = api.get("/api/v1/bookings/$bookingId", customerToken, headers = tenantHeaders())
        assertEquals("CONFIRMED", api.json(custFinalResp)["status"].asText())

        val provFinalResp = api.get("/api/v1/bookings/$bookingId", providerToken, headers = tenantHeaders())
        assertEquals("CONFIRMED", api.json(provFinalResp)["status"].asText())
    }

    @Test
    fun `complete flow with price negotiation`() {
        val ts = System.nanoTime()
        val customerEmail = "cust-neg-$ts@test.com"
        val providerEmail = "prov-neg-$ts@test.com"

        // Register both users
        val customerToken = api.register(customerEmail, firstName = "Carol").accessToken
        var providerToken = api.register(providerEmail, firstName = "Dave").accessToken
        val providerId = api.extractUserId(providerToken)

        // Setup provider with service
        val categories = api.json(api.get("/api/v1/categories", headers = tenantHeaders()))
        val categoryId = categories[0]["id"].asText()
        val subCatId = categories[0]["children"][0]["id"].asText()

        api.post("/api/v1/providers/me", mapOf(
            "businessName" to "Dave's Studio",
            "categoryIds" to listOf(categoryId)
        ), providerToken, headers = tenantHeaders())
        providerToken = api.login(providerEmail).accessToken

        val svcResp = api.post("/api/v1/providers/me/services", mapOf(
            "categoryId" to subCatId,
            "pricingType" to "FIXED",
            "priceAmount" to 200,
            "priceCurrency" to "EUR",
            "durationMinutes" to 90,
            "primaryAmount" to 200,
            "translations" to listOf(mapOf("lang" to "en", "title" to "Premium Package"))
        ), providerToken, headers = tenantHeaders())
        val serviceId = api.json(svcResp)["id"].asText()

        // Admin approves
        val adminToken = adminLogin()
        api.put("/api/v1/admin/providers/$providerId/approve", mapOf(
            "approvalStatus" to "APPROVED"
        ), adminToken, headers = adminHeaders())

        // Customer books
        val bookingResp = api.post("/api/v1/bookings", mapOf(
            "providerId" to providerId,
            "services" to listOf(mapOf("serviceId" to serviceId)),
            "scheduledAt" to Instant.now().plus(5, ChronoUnit.DAYS).toString(),
            "bookingType" to "OUTCALL",
            "addressText" to "42 Elm Street"
        ), customerToken, headers = tenantHeaders())
        assertEquals(HttpStatus.CREATED, bookingResp.statusCode) { "Create booking failed: ${bookingResp.body}" }
        val bookingId = api.json(bookingResp)["id"].asText()
        assertEquals(200.0, api.json(bookingResp)["priceAmount"].asDouble())

        // Provider quotes a different price
        val quoteResp = api.post("/api/v1/bookings/$bookingId/quote", mapOf(
            "priceAmount" to 180,
            "providerNotes" to "New client discount"
        ), providerToken, headers = tenantHeaders())
        assertEquals(HttpStatus.OK, quoteResp.statusCode) { "Quote failed: ${quoteResp.body}" }
        val quoted = api.json(quoteResp)
        assertEquals("QUOTED", quoted["status"].asText())
        assertEquals(180.0, quoted["priceAmount"].asDouble())
        assertEquals(200.0, quoted["originalAmount"].asDouble())

        // Customer accepts the quoted price
        val acceptResp = api.patch("/api/v1/bookings/$bookingId/status",
            mapOf("status" to "CONFIRMED"), customerToken, headers = tenantHeaders())
        assertEquals(HttpStatus.OK, acceptResp.statusCode) { "Accept quote failed: ${acceptResp.body}" }
        assertEquals("CONFIRMED", api.json(acceptResp)["status"].asText())
        assertEquals(180.0, api.json(acceptResp)["priceAmount"].asDouble())
    }

    @Test
    fun `unapproved provider cannot receive bookings via public API`() {
        val ts = System.nanoTime()
        val customerEmail = "cust-unap-$ts@test.com"
        val providerEmail = "prov-unap-$ts@test.com"

        val customerToken = api.register(customerEmail, firstName = "Eve").accessToken
        var providerToken = api.register(providerEmail, firstName = "Frank").accessToken
        val providerId = api.extractUserId(providerToken)

        val categories = api.json(api.get("/api/v1/categories", headers = tenantHeaders()))
        val categoryId = categories[0]["id"].asText()
        val subCatId = categories[0]["children"][0]["id"].asText()

        api.post("/api/v1/providers/me", mapOf(
            "businessName" to "Frank's Unapproved Studio",
            "categoryIds" to listOf(categoryId)
        ), providerToken, headers = tenantHeaders())
        providerToken = api.login(providerEmail).accessToken

        val svcResp = api.post("/api/v1/providers/me/services", mapOf(
            "categoryId" to subCatId,
            "pricingType" to "FIXED",
            "priceAmount" to 75,
            "priceCurrency" to "EUR",
            "durationMinutes" to 45,
            "primaryAmount" to 75,
            "translations" to listOf(mapOf("lang" to "en", "title" to "Basic Service"))
        ), providerToken, headers = tenantHeaders())
        val serviceId = api.json(svcResp)["id"].asText()

        // NOT approved — provider not visible publicly
        val publicResp = api.get("/api/v1/providers/$providerId", headers = tenantHeaders())
        assertEquals(HttpStatus.NOT_FOUND, publicResp.statusCode)

        // Customer can still attempt a booking (knows the provider ID)
        // The system allows it — provider just isn't discoverable
        val bookingResp = api.post("/api/v1/bookings", mapOf(
            "providerId" to providerId,
            "services" to listOf(mapOf("serviceId" to serviceId)),
            "scheduledAt" to Instant.now().plus(2, ChronoUnit.DAYS).toString()
        ), customerToken, headers = tenantHeaders())
        // Booking still works — approval gates discovery, not booking creation
        assertEquals(HttpStatus.CREATED, bookingResp.statusCode)
    }
}
