package com.hps.api.auth

import com.hps.common.exception.BadRequestException
import com.hps.common.exception.UnauthorizedException
import com.hps.common.tenant.TenantContext
import com.hps.domain.user.User
import com.hps.domain.user.UserProfile
import com.hps.persistence.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val HANDLE_REGEX = Regex("^[a-z][a-z0-9_]{2,29}$")

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {
    @Transactional
    fun register(request: RegisterRequest): TokenResponse {
        val tenantId = TenantContext.get()

        if (tenantId != null) {
            if (userRepository.existsByEmailAndTenantId(request.email, tenantId)) {
                throw BadRequestException("Email already registered")
            }
        } else {
            if (userRepository.existsByEmail(request.email)) {
                throw BadRequestException("Email already registered")
            }
        }

        // Validate and check handle uniqueness
        val handle = request.handle?.lowercase()
        if (handle != null) {
            val reason = validateHandle(handle, tenantId)
            if (reason != null) throw BadRequestException(reason)
        }

        val user = User(
            email = request.email,
            tenantId = tenantId,
            passwordHash = passwordEncoder.encode(request.password),
            handle = handle,
            preferredLang = request.preferredLang
        )

        if (request.firstName != null || request.lastName != null) {
            user.profile = UserProfile(
                user = user,
                firstName = request.firstName,
                lastName = request.lastName
            )
        }

        userRepository.save(user)

        return generateTokens(user)
    }

    fun login(request: LoginRequest): TokenResponse {
        val tenantId = TenantContext.get()

        val user = if (tenantId != null) {
            userRepository.findByEmailAndTenantId(request.email, tenantId)
                ?: userRepository.findByEmail(request.email)
                    ?.takeIf { it.tenantId == null }
        } else {
            userRepository.findByEmail(request.email)
        } ?: throw UnauthorizedException("Invalid email or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw UnauthorizedException("Invalid email or password")
        }

        if (!user.isActive) {
            throw UnauthorizedException("Account is disabled")
        }

        return generateTokens(user)
    }

    fun checkHandleAvailable(handle: String, tenantId: UUID?): HandleAvailableResponse {
        val h = handle.lowercase()
        val reason = validateHandle(h, tenantId)
        return if (reason != null) {
            HandleAvailableResponse(available = false, reason = reason)
        } else {
            HandleAvailableResponse(available = true)
        }
    }

    fun refresh(request: RefreshRequest): TokenResponse {
        val claims = jwtService.validateToken(request.refreshToken)
            ?: throw UnauthorizedException("Invalid refresh token")

        val user = userRepository.findByEmail(claims["email"] as String)
            ?: throw UnauthorizedException("User not found")

        if (!user.isActive) {
            throw UnauthorizedException("Account is disabled")
        }

        return generateTokens(user)
    }

    /**
     * Returns null if handle is valid, or an error reason string.
     */
    private fun validateHandle(handle: String, tenantId: UUID?): String? {
        if (!HANDLE_REGEX.matches(handle)) {
            return "Handle must be 3-30 characters: lowercase letters, numbers, underscores. Must start with a letter."
        }
        if (handle == "admin" || handle.contains("admin")) {
            return "Handle cannot contain 'admin'"
        }
        if (tenantId != null && userRepository.existsByHandleAndTenantId(handle, tenantId)) {
            return "Handle is already taken"
        }
        return null
    }

    private fun generateTokens(user: User): TokenResponse {
        val userId = user.id.toString()
        val tenantStr = user.tenantId?.toString()
        return TokenResponse(
            accessToken = jwtService.generateAccessToken(userId, user.email, user.role.name, tenantStr),
            refreshToken = jwtService.generateRefreshToken(userId, user.email, user.role.name, tenantStr),
            expiresIn = jwtService.getAccessExpirationMs() / 1000
        )
    }
}
