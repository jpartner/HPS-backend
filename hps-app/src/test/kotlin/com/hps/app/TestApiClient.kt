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

    fun get(path: String, token: String? = null, lang: String = "en"): ResponseEntity<String> {
        val headers = HttpHeaders().apply {
            accept = listOf(MediaType.APPLICATION_JSON)
            set("Accept-Language", lang)
            token?.let { setBearerAuth(it) }
        }
        return rest.exchange(path, HttpMethod.GET, HttpEntity<Any>(headers), String::class.java)
    }

    fun post(path: String, body: Any?, token: String? = null, lang: String = "en"): ResponseEntity<String> {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
            set("Accept-Language", lang)
            token?.let { setBearerAuth(it) }
        }
        return rest.exchange(path, HttpMethod.POST, HttpEntity(body, headers), String::class.java)
    }

    fun put(path: String, body: Any?, token: String? = null): ResponseEntity<String> {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
            token?.let { setBearerAuth(it) }
        }
        return rest.exchange(path, HttpMethod.PUT, HttpEntity(body, headers), String::class.java)
    }

    fun patch(path: String, body: Any?, token: String? = null): ResponseEntity<String> {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
            token?.let { setBearerAuth(it) }
        }
        return rest.exchange(path, HttpMethod.PATCH, HttpEntity(body, headers), String::class.java)
    }

    fun delete(path: String, token: String? = null): ResponseEntity<String> {
        val headers = HttpHeaders().apply {
            token?.let { setBearerAuth(it) }
        }
        return rest.exchange(path, HttpMethod.DELETE, HttpEntity<Any>(headers), String::class.java)
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

    data class TokenPair(val accessToken: String, val refreshToken: String)
}
