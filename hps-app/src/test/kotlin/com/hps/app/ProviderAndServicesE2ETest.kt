package com.hps.app

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ProviderAndServicesE2ETest : BaseE2ETest() {

    private lateinit var providerToken: String
    private lateinit var providerId: String
    private lateinit var massageCategoryId: String
    private lateinit var relaxCategoryId: String
    private lateinit var warsawId: String

    @BeforeEach
    fun setup() {
        val email = "provider-${System.nanoTime()}@test.com"
        api.register(email, firstName = "Provider")

        // Get category IDs
        val categories = api.json(api.get("/api/v1/categories"))
        massageCategoryId = categories[0]["id"].asText()
        relaxCategoryId = categories[0]["children"][0]["id"].asText()

        // Get Warsaw city ID
        val plRegions = api.json(api.get("/api/v1/countries/PL/regions"))
        val mazRegion = plRegions.first {
            val name = it["name"].asText().lowercase()
            name.contains("masov") || name.contains("mazow")
        }
        val cities = api.json(api.get("/api/v1/regions/${mazRegion["id"].asText()}/cities"))
        warsawId = cities.first {
            val name = it["name"].asText().lowercase()
            name.contains("warsz") || name.contains("warsaw")
        }["id"].asText()

        // Create provider profile
        val token = api.login(email).accessToken
        api.post("/api/v1/providers/me", mapOf(
            "businessName" to "Test Spa",
            "description" to "A lovely test spa",
            "cityId" to warsawId,
            "latitude" to 52.2297,
            "longitude" to 21.0122,
            "isMobile" to true,
            "categoryIds" to listOf(massageCategoryId)
        ), token)

        // Re-login to get PROVIDER role
        providerToken = api.login(email).accessToken

        // Extract provider ID from token
        providerId = api.extractUserId(providerToken)
    }

    @Test
    fun `provider profile is visible in public listings`() {
        val resp = api.get("/api/v1/providers?categoryId=$massageCategoryId")
        assertEquals(HttpStatus.OK, resp.statusCode)
        val data = api.json(resp)["data"]
        assertTrue(data.any { it["id"].asText() == providerId })
    }

    @Test
    fun `provider detail includes city name translated`() {
        val enResp = api.get("/api/v1/providers/$providerId", lang = "en")
        assertEquals(HttpStatus.OK, enResp.statusCode)
        val enDetail = api.json(enResp)
        assertTrue(
            enDetail["cityName"].asText().contains("Warsaw") ||
            enDetail["cityName"].asText().contains("Warszawa"),
            "City should be Warsaw/Warszawa"
        )

        val plResp = api.get("/api/v1/providers/$providerId", lang = "pl")
        val plDetail = api.json(plResp)
        assertEquals("Warszawa", plDetail["cityName"].asText())
    }

    @Test
    fun `provider can filter by city`() {
        val resp = api.get("/api/v1/providers?categoryId=$massageCategoryId&cityId=$warsawId")
        val data = api.json(resp)["data"]
        assertTrue(data.size() > 0)
        assertTrue(data.all {
            val name = it["cityName"].asText().lowercase()
            name.contains("warsaw") || name.contains("warszawa")
        })
    }

    @Test
    fun `provider can create and manage services`() {
        // Create service
        val createResp = api.post("/api/v1/providers/me/services", mapOf(
            "categoryId" to relaxCategoryId,
            "pricingType" to "FIXED",
            "priceAmount" to 200,
            "priceCurrency" to "PLN",
            "durationMinutes" to 90,
            "translations" to listOf(
                mapOf("lang" to "en", "title" to "Deep Tissue", "description" to "90min deep tissue"),
                mapOf("lang" to "pl", "title" to "Głęboki tkankowy", "description" to "90min masaż głęboki")
            )
        ), providerToken)
        assertEquals(HttpStatus.CREATED, createResp.statusCode)
        val service = api.json(createResp)
        val serviceId = service["id"].asText()
        assertEquals("Deep Tissue", service["title"].asText())
        assertEquals(200.0, service["priceAmount"].asDouble())

        // List provider services
        val listResp = api.get("/api/v1/providers/$providerId/services")
        assertEquals(HttpStatus.OK, listResp.statusCode)
        val services = api.json(listResp)
        assertTrue(services.any { it["id"].asText() == serviceId })

        // Get service in Polish
        val plResp = api.get("/api/v1/services/$serviceId", lang = "pl")
        assertEquals("Głęboki tkankowy", api.json(plResp)["title"].asText())

        // Update service
        val updateResp = api.put("/api/v1/providers/me/services/$serviceId", mapOf(
            "priceAmount" to 220,
            "translations" to listOf(
                mapOf("lang" to "en", "title" to "Deep Tissue Premium"),
                mapOf("lang" to "pl", "title" to "Głęboki tkankowy Premium")
            )
        ), providerToken)
        assertEquals(HttpStatus.OK, updateResp.statusCode) { "Update failed: ${updateResp.body}" }
        assertEquals(220.0, api.json(updateResp)["priceAmount"].asDouble())

        // Delete service
        val delResp = api.delete("/api/v1/providers/me/services/$serviceId", providerToken)
        assertEquals(HttpStatus.NO_CONTENT, delResp.statusCode)
    }

    @Test
    fun `another user cannot modify provider's services`() {
        // Create a service first
        val svcResp = api.post("/api/v1/providers/me/services", mapOf(
            "categoryId" to relaxCategoryId,
            "pricingType" to "HOURLY",
            "priceAmount" to 100,
            "priceCurrency" to "PLN",
            "durationMinutes" to 60,
            "translations" to listOf(mapOf("lang" to "en", "title" to "Quick Massage"))
        ), providerToken)
        val serviceId = api.json(svcResp)["id"].asText()

        // Different user
        val otherToken = api.register("other-${System.nanoTime()}@test.com").accessToken
        val delResp = api.delete("/api/v1/providers/me/services/$serviceId", otherToken)
        // Should fail because the other user is not a provider or doesn't own this service
        assertTrue(delResp.statusCode.is4xxClientError)
    }
}
