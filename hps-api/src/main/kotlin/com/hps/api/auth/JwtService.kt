package com.hps.api.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${hps.jwt.secret}") private val secret: String,
    @Value("\${hps.jwt.expiration-ms}") private val expirationMs: Long,
    @Value("\${hps.jwt.refresh-expiration-ms}") private val refreshExpirationMs: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateAccessToken(userId: String, email: String, role: String): String =
        buildToken(userId, email, role, expirationMs)

    fun generateRefreshToken(userId: String, email: String, role: String): String =
        buildToken(userId, email, role, refreshExpirationMs)

    fun getAccessExpirationMs(): Long = expirationMs

    fun validateToken(token: String): Claims? =
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        } catch (e: Exception) {
            null
        }

    private fun buildToken(userId: String, email: String, role: String, expiration: Long): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .claim("role", role)
            .issuedAt(now)
            .expiration(Date(now.time + expiration))
            .signWith(key)
            .compact()
    }
}
