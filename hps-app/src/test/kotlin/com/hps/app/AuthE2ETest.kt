package com.hps.app

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class AuthE2ETest : BaseE2ETest() {

    @Test
    fun `register and login flow`() {
        val tokens = api.register("auth-test@example.com", firstName = "AuthTest")
        assertTrue(tokens.accessToken.isNotEmpty())
        assertTrue(tokens.refreshToken.isNotEmpty())

        val loginTokens = api.login("auth-test@example.com")
        assertTrue(loginTokens.accessToken.isNotEmpty())
    }

    @Test
    fun `duplicate email returns 400`() {
        api.register("dup@example.com")
        val resp = api.post("/api/v1/auth/register", mapOf(
            "email" to "dup@example.com",
            "password" to "password123"
        ))
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }

    @Test
    fun `wrong password returns 401`() {
        api.register("wrongpw@example.com")
        val resp = api.post("/api/v1/auth/login", mapOf(
            "email" to "wrongpw@example.com",
            "password" to "wrongpassword"
        ))
        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
    }

    @Test
    fun `protected endpoints reject unauthenticated requests`() {
        val resp = api.post("/api/v1/bookings", mapOf("providerId" to "fake"))
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
    }

    @Test
    fun `token refresh works`() {
        val tokens = api.register("refresh@example.com")
        val resp = api.post("/api/v1/auth/refresh", mapOf("refreshToken" to tokens.refreshToken))
        assertEquals(HttpStatus.OK, resp.statusCode)
        val newTokens = api.json(resp)
        assertTrue(newTokens["accessToken"].asText().isNotEmpty())
    }
}
