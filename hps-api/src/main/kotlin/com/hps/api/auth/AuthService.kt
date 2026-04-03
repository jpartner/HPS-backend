package com.hps.api.auth

import com.hps.common.exception.BadRequestException
import com.hps.common.exception.UnauthorizedException
import com.hps.domain.user.User
import com.hps.domain.user.UserProfile
import com.hps.persistence.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {
    @Transactional
    fun register(request: RegisterRequest): TokenResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw BadRequestException("Email already registered")
        }

        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
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
        val user = userRepository.findByEmail(request.email)
            ?: throw UnauthorizedException("Invalid email or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw UnauthorizedException("Invalid email or password")
        }

        if (!user.isActive) {
            throw UnauthorizedException("Account is disabled")
        }

        return generateTokens(user)
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

    private fun generateTokens(user: User): TokenResponse {
        val userId = user.id.toString()
        return TokenResponse(
            accessToken = jwtService.generateAccessToken(userId, user.email, user.role.name),
            refreshToken = jwtService.generateRefreshToken(userId, user.email, user.role.name),
            expiresIn = jwtService.getAccessExpirationMs() / 1000
        )
    }
}
