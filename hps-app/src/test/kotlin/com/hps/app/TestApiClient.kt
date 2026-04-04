package com.hps.app

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*

/**
 * Helper for making authenticated API calls in e2e tests.
 */
class TestApiClient(
    private val rest: TestRestTemplate,
    private val mapper: ObjectMapper
) {
    // Default API key for the default tenant (seeded by V18 migration)
    private val defaultHeaders = mapOf(
        "X-Client-Id" to "11111111-1111-1111-1111-111111111111",
        "X-Client-Secret" to "hps-dev-secret-key"
    )

    fun register(email: String, password: String = "password123", firstName: String = "Test"): TokenPair {
        val body = mapOf(
            "email" to email,
            "password" to password,
            "firstName" to firstName
        )
        val resp = post("/api/v1/auth/register", body)
        assert(resp.statusCode == HttpStatus.CREATED) { "Register failed: ${resp.statusCode} ${resp.body}" }
        val json = mapper.readTree(resp.body)
        return TokenPair(json["accessToken"].asText(), json["refreshToken"].asText())
    }

    fun login(email: String, password: String = "password123"): TokenPair {
        val body = mapOf("email" to email, "password" to password)
        val resp = post("/api/v1/auth/login", body)
        assert(resp.statusCode == HttpStatus.OK) { "Login failed: ${resp.statusCode}" }
        val json = mapper.readTree(resp.body)
        return TokenPair(json["accessToken"].asText(), json["refreshToken"].asText())
    }

    fun get(
        path: String,
        token: String? = null,
        lang: String = "en",
        headers: Map<String, String> = defaultHeaders
    ): ResponseEntity<String> {
        val httpHeaders = buildHeaders(token, lang, headers)
        return rest.exchange(path, HttpMethod.GET, HttpEntity<Any>(httpHeaders), String::class.java)
    }

    fun post(
        path: String,
        body: Any?,
        token: String? = null,
        lang: String = "en",
        headers: Map<String, String> = defaultHeaders
    ): ResponseEntity<String> {
        val httpHeaders = buildHeaders(token, lang, headers).apply {
            contentType = MediaType.APPLICATION_JSON
        }
        return rest.exchange(path, HttpMethod.POST, HttpEntity(body, httpHeaders), String::class.java)
    }

    fun put(
        path: String,
        body: Any?,
        token: String? = null,
        lang: String = "en",
        headers: Map<String, String> = defaultHeaders
    ): ResponseEntity<String> {
        val httpHeaders = buildHeaders(token, lang, headers).apply {
            contentType = MediaType.APPLICATION_JSON
        }
        return rest.exchange(path, HttpMethod.PUT, HttpEntity(body, httpHeaders), String::class.java)
    }

    fun patch(
        path: String,
        body: Any?,
        token: String? = null,
        lang: String = "en",
        headers: Map<String, String> = defaultHeaders
    ): ResponseEntity<String> {
        val httpHeaders = buildHeaders(token, lang, headers).apply {
            contentType = MediaType.APPLICATION_JSON
        }
        return rest.exchange(path, HttpMethod.PATCH, HttpEntity(body, httpHeaders), String::class.java)
    }

    fun delete(
        path: String,
        token: String? = null,
        headers: Map<String, String> = defaultHeaders
    ): ResponseEntity<String> {
        val httpHeaders = buildHeaders(token, "en", headers)
        return rest.exchange(path, HttpMethod.DELETE, HttpEntity<Any>(httpHeaders), String::class.java)
    }

    fun json(response: ResponseEntity<String>): JsonNode = mapper.readTree(response.body)

    fun extractUserId(token: String): String {
        val parts = token.split(".")
        val padded = parts[1].let { s ->
            val r = s.length % 4
            if (r == 0) s else s + "=".repeat(4 - r)
        }
        val payloadJson = String(java.util.Base64.getUrlDecoder().decode(padded))
        return mapper.readTree(payloadJson)["sub"].asText()
    }

    private fun buildHeaders(
        token: String?,
        lang: String,
        extra: Map<String, String>
    ): HttpHeaders {
        return HttpHeaders().apply {
            accept = listOf(MediaType.APPLICATION_JSON)
            set("Accept-Language", lang)
            token?.let { setBearerAuth(it) }
            extra.forEach { (k, v) -> set(k, v) }
        }
    }

    data class TokenPair(val accessToken: String, val refreshToken: String)
}
