package com.hps.app

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class GeoAndCategoriesE2ETest : BaseE2ETest() {

    @Test
    fun `seed data is loaded on startup`() {
        val countries = api.get("/api/v1/countries")
        assertEquals(HttpStatus.OK, countries.statusCode)
        val list = api.json(countries)
        assertEquals(10, list.size(), "Expected 10 seeded countries")
    }

    @Test
    fun `countries are translated by Accept-Language`() {
        val enResp = api.get("/api/v1/countries", lang = "en")
        val plResp = api.get("/api/v1/countries", lang = "pl")
        val ukResp = api.get("/api/v1/countries", lang = "uk")

        val enCountries = api.json(enResp)
        val plCountries = api.json(plResp)
        val ukCountries = api.json(ukResp)

        // Find Poland in each
        val enPoland = enCountries.first { it["isoCode"].asText() == "PL" }["name"].asText()
        val plPoland = plCountries.first { it["isoCode"].asText() == "PL" }["name"].asText()
        val ukPoland = ukCountries.first { it["isoCode"].asText() == "PL" }["name"].asText()

        assertEquals("Poland", enPoland)
        assertEquals("Polska", plPoland)
        assertTrue(ukPoland.isNotEmpty(), "Ukrainian translation should exist")
        assertNotEquals(enPoland, ukPoland, "Ukrainian should differ from English")
    }

    @Test
    fun `can drill down country to regions to cities`() {
        val countries = api.json(api.get("/api/v1/countries"))
        val germany = countries.first { it["isoCode"].asText() == "DE" }
        val deCode = germany["isoCode"].asText()

        val regions = api.json(api.get("/api/v1/countries/$deCode/regions"))
        assertTrue(regions.size() >= 16, "Germany should have at least 16 Bundesländer")

        val firstRegionId = regions[0]["id"].asText()
        val cities = api.json(api.get("/api/v1/regions/$firstRegionId/cities"))
        assertTrue(cities.size() > 0, "Region should have cities")

        val city = cities[0]
        assertNotNull(city["name"])
        assertNotNull(city["latitude"])
        assertNotNull(city["longitude"])
    }

    @Test
    fun `categories are loaded as a tree with translations`() {
        val enCategories = api.json(api.get("/api/v1/categories", lang = "en"))
        assertTrue(enCategories.size() >= 8, "Should have at least 8 root categories")

        val massage = enCategories.first { it["name"].asText() == "Massage" }
        assertNotNull(massage["icon"])
        assertTrue(massage["children"].size() >= 5, "Massage should have subcategories")

        // Check Polish translations
        val plCategories = api.json(api.get("/api/v1/categories", lang = "pl"))
        val masaz = plCategories.first { it["name"].asText() == "Masaż" }
        assertTrue(masaz["children"].size() >= 5)
    }
}
