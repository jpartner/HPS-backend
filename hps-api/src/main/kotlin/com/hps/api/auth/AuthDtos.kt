package com.hps.api.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 100)
    val password: String,

    @field:Pattern(regexp = "^[a-z][a-z0-9_]{2,29}$", message = "Handle must be 3-30 lowercase letters, numbers, underscores")
    val handle: String? = null,

    val firstName: String? = null,
    val lastName: String? = null,
    val preferredLang: String = "en"
)

data class HandleAvailableResponse(
    val available: Boolean,
    val reason: String? = null
)

data class LoginRequest(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    val password: String
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

data class RefreshRequest(
    @field:NotBlank
    val refreshToken: String
)
