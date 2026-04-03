package com.hps.app

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class SchedulingE2ETest : BaseE2ETest() {

    private lateinit var providerToken: String
    private lateinit var providerId: String
    private lateinit var serviceId: String

    @BeforeEach
    fun setup() {
        val email = "sched-${System.nanoTime()}@test.com"
        api.register(email, firstName = "Scheduler")

        val categories = api.json(api.get("/api/v1/categories"))
        val categoryId = categories[0]["id"].asText()
        val subCategoryId = categories[0]["children"][0]["id"].asText()

        api.post("/api/v1/providers/me", mapOf(
            "businessName" to "Schedule Test Spa",
            "categoryIds" to listOf(categoryId)
        ), api.login(email).accessToken)

        providerToken = api.login(email).accessToken
        providerId = api.extractUserId(providerToken)

        // Create a 90-minute service
        val svcResp = api.post("/api/v1/providers/me/services", mapOf(
            "categoryId" to subCategoryId,
            "pricingType" to "FIXED",
            "priceAmount" to 150,
            "priceCurrency" to "PLN",
            "durationMinutes" to 90,
            "translations" to listOf(mapOf("lang" to "en", "title" to "Test Massage 90min"))
        ), providerToken)
        serviceId = api.json(svcResp)["id"].asText()
    }

    @Test
    fun `set and get weekly schedule`() {
        val schedule = mapOf(
            "timezone" to "Europe/Warsaw",
            "incallGapMinutes" to 15,
            "outcallGapMinutes" to 60,
            "minLeadTimeHours" to 1,
            "slots" to listOf(
                mapOf("dayOfWeek" to 1, "startTime" to "09:00", "endTime" to "17:00"),
                mapOf("dayOfWeek" to 2, "startTime" to "09:00", "endTime" to "12:00"),
                mapOf("dayOfWeek" to 2, "startTime" to "13:00", "endTime" to "18:00"),
                mapOf("dayOfWeek" to 3, "startTime" to "10:00", "endTime" to "20:00")
            )
        )

        val putResp = api.put("/api/v1/providers/me/schedule/weekly", schedule, providerToken)
        assertEquals(HttpStatus.OK, putResp.statusCode)
        val saved = api.json(putResp)
        assertEquals("Europe/Warsaw", saved["timezone"].asText())
        assertEquals(15, saved["incallGapMinutes"].asInt())
        assertEquals(4, saved["slots"].size())

        val getResp = api.get("/api/v1/providers/me/schedule/weekly", providerToken)
        assertEquals(HttpStatus.OK, getResp.statusCode)
        assertEquals(4, api.json(getResp)["slots"].size())
    }

    @Test
    fun `availability reflects weekly schedule`() {
        // Set Mon-Fri 09:00-17:00
        api.put("/api/v1/providers/me/schedule/weekly", mapOf(
            "timezone" to "Europe/Warsaw",
            "incallGapMinutes" to 0,
            "outcallGapMinutes" to 0,
            "minLeadTimeHours" to 0,
            "slots" to (1..5).map { day ->
                mapOf("dayOfWeek" to day, "startTime" to "09:00", "endTime" to "17:00")
            }
        ), providerToken)

        // Query a future Monday-Sunday
        // Find next Monday that's at least a week away
        val nextMonday = java.time.LocalDate.now().plusWeeks(2)
            .with(java.time.DayOfWeek.MONDAY)
        val nextSunday = nextMonday.plusDays(6)

        val resp = api.get(
            "/api/v1/providers/$providerId/availability" +
            "?from=$nextMonday&to=$nextSunday&serviceId=$serviceId"
        )
        assertEquals(HttpStatus.OK, resp.statusCode)
        val availability = api.json(resp)
        val dates = availability["dates"]
        assertEquals(7, dates.size(), "Should return 7 days")

        // Mon-Fri should have slots, Sat-Sun should be empty
        for (i in 0..4) {
            assertTrue(dates[i]["slots"].size() > 0, "Weekday $i should have slots")
        }
        assertEquals(0, dates[5]["slots"].size(), "Saturday should be empty")
        assertEquals(0, dates[6]["slots"].size(), "Sunday should be empty")

        // 90min service in 09:00-17:00 → last slot at 15:30 (3 consecutive 30min blocks)
        val mondaySlots = dates[0]["slots"]
        val lastSlot = mondaySlots[mondaySlots.size() - 1].asText()
        assertEquals("15:30:00", lastSlot)
    }

    @Test
    fun `date override marks day unavailable`() {
        api.put("/api/v1/providers/me/schedule/weekly", mapOf(
            "timezone" to "Europe/Warsaw",
            "incallGapMinutes" to 0,
            "outcallGapMinutes" to 0,
            "minLeadTimeHours" to 0,
            "slots" to (1..5).map { day ->
                mapOf("dayOfWeek" to day, "startTime" to "09:00", "endTime" to "17:00")
            }
        ), providerToken)

        val nextMonday = java.time.LocalDate.now().plusWeeks(2)
            .with(java.time.DayOfWeek.MONDAY)

        // Mark Monday as unavailable
        val overrideResp = api.put(
            "/api/v1/providers/me/schedule/overrides/$nextMonday",
            mapOf("isUnavailable" to true, "reason" to "Holiday"),
            providerToken
        )
        assertEquals(HttpStatus.OK, overrideResp.statusCode)

        // Check availability
        val resp = api.get(
            "/api/v1/providers/$providerId/availability" +
            "?from=$nextMonday&to=$nextMonday&serviceId=$serviceId"
        )
        val dates = api.json(resp)["dates"]
        assertEquals(0, dates[0]["slots"].size(), "Override day should have no slots")

        // Delete override
        api.delete("/api/v1/providers/me/schedule/overrides/$nextMonday", providerToken)

        // Slots should return
        val resp2 = api.get(
            "/api/v1/providers/$providerId/availability" +
            "?from=$nextMonday&to=$nextMonday&serviceId=$serviceId"
        )
        assertTrue(api.json(resp2)["dates"][0]["slots"].size() > 0, "Should have slots after override removed")
    }

    @Test
    fun `rejects invalid 30min alignment`() {
        val resp = api.put("/api/v1/providers/me/schedule/weekly", mapOf(
            "timezone" to "Europe/Warsaw",
            "slots" to listOf(
                mapOf("dayOfWeek" to 1, "startTime" to "09:15", "endTime" to "17:00")
            )
        ), providerToken)
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }
}
