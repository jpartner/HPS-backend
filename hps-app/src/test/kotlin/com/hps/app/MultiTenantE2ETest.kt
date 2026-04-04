package com.hps.app

import com.hps.domain.user.UserRole
import com.hps.persistence.user.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

class MultiTenantE2ETest : BaseE2ETest() {

    @Autowired
    lateinit var userRepository: UserRepository

    private lateinit var adminToken: String

    private fun h() = mapOf(
        "X-Client-Id" to "11111111-1111-1111-1111-111111111111",
        "X-Client-Secret" to "hps-dev-secret-key"
    )

    @BeforeEach
    fun setup() {
        val email = "admin-${System.nanoTime()}@hps.com"
        api.register(email, firstName = "Admin")
        val user = userRepository.findByEmail(email)!!
        user.role = UserRole.SUPER_ADMIN
        userRepository.save(user)
        adminToken = api.login(email).accessToken
    }

    @Test
    fun `full admin setup - categories templates attributes`() {
        val catResp = api.post("/api/v1/admin/categories", mapOf(
            "slug" to "e2e-cat-${System.nanoTime()}",
            "icon" to "heart",
            "sortOrder" to 99,
            "translations" to listOf(
                mapOf("lang" to "en", "name" to "E2E Category"),
                mapOf("lang" to "pl", "name" to "Kategoria E2E")
            )
        ), adminToken, headers = h())
        assertEquals(HttpStatus.CREATED, catResp.statusCode) { catResp.body.toString() }
        val catId = api.json(catResp)["id"].asText()

        val subResp = api.post("/api/v1/admin/categories", mapOf(
            "slug" to "e2e-sub-${System.nanoTime()}",
            "icon" to "spa",
            "sortOrder" to 1,
            "parentId" to catId,
            "translations" to listOf(mapOf("lang" to "en", "name" to "E2E Sub"))
        ), adminToken, headers = h())
        assertEquals(HttpStatus.CREATED, subResp.statusCode)
        val subId = api.json(subResp)["id"].asText()

        val cats = api.json(api.get("/api/v1/admin/categories", adminToken, headers = h()))
        assertTrue(cats.any { it["id"].asText() == catId })

        val catDetail = api.json(api.get("/api/v1/admin/categories/$catId", adminToken, headers = h()))
        assertTrue(catDetail["children"].size() > 0)

        val updateResp = api.put("/api/v1/admin/categories/$catId", mapOf(
            "icon" to "star", "sortOrder" to 99,
            "translations" to listOf(
                mapOf("lang" to "en", "name" to "Updated E2E"),
                mapOf("lang" to "pl", "name" to "Zmieniona E2E"),
                mapOf("lang" to "uk", "name" to "Оновлена E2E")
            )
        ), adminToken, headers = h())
        assertEquals(HttpStatus.OK, updateResp.statusCode)
        assertEquals(3, api.json(updateResp)["translations"].size())

        val tmplResp = api.post("/api/v1/admin/service-templates", mapOf(
            "slug" to "e2e-tmpl-${System.nanoTime()}",
            "categoryId" to subId,
            "defaultDurationMinutes" to 60,
            "sortOrder" to 1,
            "translations" to listOf(
                mapOf("lang" to "en", "title" to "E2E Service"),
                mapOf("lang" to "pl", "title" to "Usługa E2E")
            )
        ), adminToken, headers = h())
        assertEquals(HttpStatus.CREATED, tmplResp.statusCode) { tmplResp.body.toString() }

        val attrResp = api.post("/api/v1/admin/attributes", mapOf(
            "domain" to "Updated E2E",
            "key" to "e2e-attr-${System.nanoTime()}",
            "dataType" to "TEXT",
            "isRequired" to false,
            "sortOrder" to 1,
            "translations" to listOf(mapOf("lang" to "en", "label" to "E2E Attr"))
        ), adminToken, headers = h())
        assertEquals(HttpStatus.CREATED, attrResp.statusCode) { attrResp.body.toString() }

        val dash = api.json(api.get("/api/v1/admin/dashboard/stats", adminToken, headers = h()))
        assertTrue(dash["totalUsers"].asInt() >= 0)
    }

    @Test
    fun `provider management via admin`() {
        val cats = api.json(api.get("/api/v1/categories", headers = h()))
        val categoryId = cats[0]["id"].asText()

        val provEmail = "prov-e2e-${System.nanoTime()}@test.com"
        api.post("/api/v1/auth/register", mapOf(
            "email" to provEmail, "password" to "password123", "firstName" to "E2EProv"
        ), headers = h())

        var provToken = api.json(api.post("/api/v1/auth/login", mapOf(
            "email" to provEmail, "password" to "password123"
        ), headers = h()))["accessToken"].asText()

        api.post("/api/v1/providers/me", mapOf(
            "businessName" to "E2E Spa", "categoryIds" to listOf(categoryId)
        ), provToken, headers = h())

        provToken = api.json(api.post("/api/v1/auth/login", mapOf(
            "email" to provEmail, "password" to "password123"
        ), headers = h()))["accessToken"].asText()
        val providerId = api.extractUserId(provToken)

        val provList = api.json(api.get("/api/v1/admin/providers", adminToken, headers = h()))
        assertTrue(provList.any { it["id"]?.asText() == providerId })

        val verifyResp = api.put("/api/v1/admin/providers/$providerId/verify",
            mapOf("isVerified" to true), adminToken, headers = h())
        assertEquals(HttpStatus.OK, verifyResp.statusCode)
        assertTrue(api.json(verifyResp)["isVerified"].asBoolean())

        val users = api.json(api.get("/api/v1/admin/users", adminToken, headers = h()))
        val userId = users.first { it["email"]?.asText() == provEmail }["id"].asText()

        val suspend = api.put("/api/v1/admin/users/$userId/status",
            mapOf("isActive" to false), adminToken, headers = h())
        assertFalse(api.json(suspend)["isActive"].asBoolean())

        val activate = api.put("/api/v1/admin/users/$userId/status",
            mapOf("isActive" to true), adminToken, headers = h())
        assertTrue(api.json(activate)["isActive"].asBoolean())
    }

    @Test
    fun `tenant CRUD`() {
        val list = api.json(api.get("/api/v1/admin/tenants", adminToken))
        assertTrue(list.size() >= 1)

        val slug = "e2e-${System.nanoTime()}"
        val create = api.post("/api/v1/admin/tenants", mapOf(
            "name" to "E2E Tenant", "slug" to slug,
            "defaultLang" to "en", "supportedLangs" to "en,pl", "defaultCurrency" to "USD"
        ), adminToken)
        assertEquals(HttpStatus.CREATED, create.statusCode) { create.body.toString() }
        val tenantId = api.json(create)["id"].asText()

        val get = api.json(api.get("/api/v1/admin/tenants/$tenantId", adminToken))
        assertEquals(slug, get["slug"].asText())

        val update = api.put("/api/v1/admin/tenants/$tenantId",
            mapOf("name" to "Updated E2E"), adminToken)
        assertEquals("Updated E2E", api.json(update)["name"].asText())

        assertEquals(HttpStatus.OK,
            api.delete("/api/v1/admin/tenants/$tenantId", adminToken).statusCode)
    }

    @Test
    fun `API key lifecycle`() {
        val tenants = api.json(api.get("/api/v1/admin/tenants", adminToken))
        val tenantId = tenants[0]["id"].asText()

        val keyResp = api.post("/api/v1/admin/tenants/$tenantId/api-keys",
            mapOf("name" to "E2E Key"), adminToken)
        assertEquals(HttpStatus.CREATED, keyResp.statusCode) { keyResp.body.toString() }
        val key = api.json(keyResp)
        val newClientId = key["clientId"].asText()
        val newSecret = key["clientSecret"].asText()
        assertTrue(newSecret.length > 20)

        val keys = api.json(api.get("/api/v1/admin/tenants/$tenantId/api-keys", adminToken))
        assertTrue(keys.size() >= 2)

        val testResp = api.get("/api/v1/categories",
            headers = mapOf("X-Client-Id" to newClientId, "X-Client-Secret" to newSecret))
        assertEquals(HttpStatus.OK, testResp.statusCode)

        assertEquals(HttpStatus.NO_CONTENT,
            api.delete("/api/v1/admin/tenants/$tenantId/api-keys/${key["id"].asText()}", adminToken).statusCode)

        val failResp = api.get("/api/v1/categories",
            headers = mapOf("X-Client-Id" to newClientId, "X-Client-Secret" to newSecret))
        assertEquals(HttpStatus.UNAUTHORIZED, failResp.statusCode)
    }

    @Test
    fun `access control - non-admin rejected`() {
        val email = "client-${System.nanoTime()}@test.com"
        val reg = api.post("/api/v1/auth/register",
            mapOf("email" to email, "password" to "password123"), headers = h())
        val clientToken = api.json(reg)["accessToken"].asText()

        assertEquals(HttpStatus.FORBIDDEN,
            api.get("/api/v1/admin/categories", clientToken, headers = h()).statusCode)
        assertEquals(HttpStatus.FORBIDDEN,
            api.get("/api/v1/admin/users", clientToken, headers = h()).statusCode)
        assertEquals(HttpStatus.FORBIDDEN,
            api.get("/api/v1/admin/tenants", clientToken).statusCode)
    }

    @Test
    fun `public endpoints work without auth`() {
        assertEquals(HttpStatus.OK, api.get("/api/v1/categories", headers = h()).statusCode)
        assertEquals(HttpStatus.OK, api.get("/api/v1/countries").statusCode)
    }
}
