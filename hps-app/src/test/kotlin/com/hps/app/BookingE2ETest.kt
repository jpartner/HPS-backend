package com.hps.app

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

class BookingE2ETest : BaseE2ETest() {

    private lateinit var providerToken: String
    private lateinit var customerToken: String
    private lateinit var providerId: String
    private lateinit var service1Id: String
    private lateinit var service2Id: String

    @BeforeEach
    fun setup() {
        val provEmail = "bk-prov-${System.nanoTime()}@test.com"
        val custEmail = "bk-cust-${System.nanoTime()}@test.com"

        api.register(provEmail, firstName = "BookProv")
        api.register(custEmail, firstName = "BookCust")

        val categories = api.json(api.get("/api/v1/categories"))
        val categoryId = categories[0]["id"].asText()
        val subCat1 = categories[0]["children"][0]["id"].asText()
        val subCat2 = categories[0]["children"][1]["id"].asText()

        // Create provider
        api.post("/api/v1/providers/me", mapOf(
            "businessName" to "Booking Test Spa",
            "categoryIds" to listOf(categoryId)
        ), api.login(provEmail).accessToken)

        providerToken = api.login(provEmail).accessToken
        customerToken = api.login(custEmail).accessToken

        providerId = api.extractUserId(providerToken)

        // Create two services
        service1Id = api.json(api.post("/api/v1/providers/me/services", mapOf(
            "categoryId" to subCat1,
            "pricingType" to "FIXED",
            "priceAmount" to 100,
            "priceCurrency" to "EUR",
            "durationMinutes" to 60,
            "translations" to listOf(mapOf("lang" to "en", "title" to "Service A"))
        ), providerToken))["id"].asText()

        service2Id = api.json(api.post("/api/v1/providers/me/services", mapOf(
            "categoryId" to subCat2,
            "pricingType" to "FIXED",
            "priceAmount" to 80,
            "priceCurrency" to "EUR",
            "durationMinutes" to 45,
            "translations" to listOf(mapOf("lang" to "en", "title" to "Service B"))
        ), providerToken))["id"].asText()
    }

    @Test
    fun `calculate fee for multiple services`() {
        val resp = api.post(
            "/api/v1/bookings/calculate?providerId=$providerId",
            listOf(
                mapOf("serviceId" to service1Id, "quantity" to 1),
                mapOf("serviceId" to service2Id, "quantity" to 2)
            ),
            customerToken
        )
        assertEquals(HttpStatus.OK, resp.statusCode)
        val calc = api.json(resp)
        assertEquals(260.0, calc["totalAmount"].asDouble()) // 100 + 80*2
        assertEquals(150, calc["totalDurationMinutes"].asInt()) // 60 + 45*2
        assertEquals("EUR", calc["currency"].asText())
    }

    @Test
    fun `full booking flow - direct confirm`() {
        val scheduledAt = Instant.now().plus(3, ChronoUnit.DAYS).toString()

        val createResp = api.post("/api/v1/bookings", mapOf(
            "providerId" to providerId,
            "services" to listOf(mapOf("serviceId" to service1Id)),
            "scheduledAt" to scheduledAt,
            "bookingType" to "INCALL",
            "clientNotes" to "Please be on time"
        ), customerToken)
        assertEquals(HttpStatus.CREATED, createResp.statusCode)

        val booking = api.json(createResp)
        val bookingId = booking["id"].asText()
        assertEquals("REQUESTED", booking["status"].asText())
        assertEquals(100.0, booking["priceAmount"].asDouble())
        assertEquals(1, booking["services"].size())

        // Provider confirms at original price
        val confirmResp = api.patch("/api/v1/bookings/$bookingId/status",
            mapOf("status" to "CONFIRMED"), providerToken)
        assertEquals(HttpStatus.OK, confirmResp.statusCode)
        assertEquals("CONFIRMED", api.json(confirmResp)["status"].asText())
    }

    @Test
    fun `booking flow with price negotiation`() {
        val scheduledAt = Instant.now().plus(5, ChronoUnit.DAYS).toString()

        val createResp = api.post("/api/v1/bookings", mapOf(
            "providerId" to providerId,
            "services" to listOf(
                mapOf("serviceId" to service1Id),
                mapOf("serviceId" to service2Id)
            ),
            "scheduledAt" to scheduledAt,
            "bookingType" to "OUTCALL",
            "addressText" to "123 Test Street"
        ), customerToken)
        val bookingId = api.json(createResp)["id"].asText()
        assertEquals(180.0, api.json(createResp)["priceAmount"].asDouble())

        // Provider quotes different price
        val quoteResp = api.post("/api/v1/bookings/$bookingId/quote", mapOf(
            "priceAmount" to 160,
            "providerNotes" to "Combo discount applied"
        ), providerToken)
        assertEquals(HttpStatus.OK, quoteResp.statusCode)
        val quoted = api.json(quoteResp)
        assertEquals("QUOTED", quoted["status"].asText())
        assertEquals(160.0, quoted["priceAmount"].asDouble())
        assertEquals(180.0, quoted["originalAmount"].asDouble())
        assertEquals("Combo discount applied", quoted["providerNotes"].asText())

        // Customer accepts by confirming
        val acceptResp = api.patch("/api/v1/bookings/$bookingId/status",
            mapOf("status" to "CONFIRMED"), customerToken)
        assertEquals("CONFIRMED", api.json(acceptResp)["status"].asText())
        assertEquals(160.0, api.json(acceptResp)["priceAmount"].asDouble())
    }

    @Test
    fun `customer can cancel requested booking`() {
        val createResp = api.post("/api/v1/bookings", mapOf(
            "providerId" to providerId,
            "services" to listOf(mapOf("serviceId" to service1Id)),
            "scheduledAt" to Instant.now().plus(4, ChronoUnit.DAYS).toString()
        ), customerToken)
        val bookingId = api.json(createResp)["id"].asText()

        val cancelResp = api.patch("/api/v1/bookings/$bookingId/status",
            mapOf("status" to "CANCELLED_BY_CLIENT", "reason" to "Changed my mind"),
            customerToken)
        assertEquals("CANCELLED_BY_CLIENT", api.json(cancelResp)["status"].asText())
    }

    @Test
    fun `customer cannot confirm their own requested booking`() {
        val createResp = api.post("/api/v1/bookings", mapOf(
            "providerId" to providerId,
            "services" to listOf(mapOf("serviceId" to service1Id)),
            "scheduledAt" to Instant.now().plus(4, ChronoUnit.DAYS).toString()
        ), customerToken)
        val bookingId = api.json(createResp)["id"].asText()

        // Customer tries to confirm (should fail - only provider can confirm REQUESTED)
        val resp = api.patch("/api/v1/bookings/$bookingId/status",
            mapOf("status" to "CONFIRMED"), customerToken)
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }

    @Test
    fun `provider cannot access another provider's bookings`() {
        val createResp = api.post("/api/v1/bookings", mapOf(
            "providerId" to providerId,
            "services" to listOf(mapOf("serviceId" to service1Id)),
            "scheduledAt" to Instant.now().plus(4, ChronoUnit.DAYS).toString()
        ), customerToken)
        val bookingId = api.json(createResp)["id"].asText()

        // Another provider
        val otherEmail = "other-prov-${System.nanoTime()}@test.com"
        val otherToken = api.register(otherEmail).accessToken

        val resp = api.get("/api/v1/bookings/$bookingId", otherToken)
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
    }

    @Test
    fun `list bookings returns role-appropriate results`() {
        api.post("/api/v1/bookings", mapOf(
            "providerId" to providerId,
            "services" to listOf(mapOf("serviceId" to service1Id)),
            "scheduledAt" to Instant.now().plus(6, ChronoUnit.DAYS).toString()
        ), customerToken)

        val custBookings = api.json(api.get("/api/v1/bookings", customerToken))
        assertTrue(custBookings.size() > 0, "Customer should see their bookings")

        val provBookings = api.json(api.get("/api/v1/bookings", providerToken))
        assertTrue(provBookings.size() > 0, "Provider should see their bookings")
    }
}
