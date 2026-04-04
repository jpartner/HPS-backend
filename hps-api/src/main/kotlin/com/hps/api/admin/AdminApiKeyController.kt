package com.hps.api.admin

import com.hps.common.exception.NotFoundException
import com.hps.domain.tenant.ApiKey
import com.hps.persistence.tenant.ApiKeyRepository
import com.hps.persistence.tenant.TenantRepository
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

// --- DTOs ---

data class ApiKeyDto(
    val id: UUID,
    val clientId: UUID,
    val tenantId: UUID,
    val name: String,
    val isActive: Boolean,
    val permissions: String,
    val createdAt: Instant,
    val lastUsedAt: Instant?
)

data class CreateApiKeyRequest(
    val name: String,
    val permissions: String = "{}"
)

data class ApiKeyCreatedResponse(
    val id: UUID,
    val clientId: UUID,
    val clientSecret: String,
    val tenantId: UUID,
    val name: String,
    val permissions: String,
    val createdAt: Instant
)

// --- Controller ---

@RestController
@RequestMapping("/api/v1/admin/tenants/{tenantId}/api-keys")
class AdminApiKeyController(
    private val apiKeyRepository: ApiKeyRepository,
    private val tenantRepository: TenantRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @GetMapping
    fun listApiKeys(@PathVariable tenantId: UUID): List<ApiKeyDto> {
        if (!tenantRepository.existsById(tenantId)) {
            throw NotFoundException("Tenant", tenantId)
        }
        return apiKeyRepository.findByTenantId(tenantId).map { it.toDto() }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun createApiKey(
        @PathVariable tenantId: UUID,
        @RequestBody request: CreateApiKeyRequest
    ): ApiKeyCreatedResponse {
        val tenant = tenantRepository.findById(tenantId)
            .orElseThrow { NotFoundException("Tenant", tenantId) }

        val clientId = UUID.randomUUID()
        val clientSecret = generateSecureSecret()
        val secretHash = passwordEncoder.encode(clientSecret)

        val apiKey = ApiKey(
            clientId = clientId,
            clientSecretHash = secretHash,
            tenant = tenant,
            name = request.name,
            permissions = request.permissions
        )
        val saved = apiKeyRepository.save(apiKey)

        return ApiKeyCreatedResponse(
            id = saved.id,
            clientId = clientId,
            clientSecret = clientSecret,
            tenantId = tenantId,
            name = saved.name,
            permissions = saved.permissions,
            createdAt = saved.createdAt
        )
    }

    @DeleteMapping("/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun deleteApiKey(
        @PathVariable tenantId: UUID,
        @PathVariable keyId: UUID
    ) {
        val apiKey = apiKeyRepository.findById(keyId)
            .orElseThrow { NotFoundException("ApiKey", keyId) }
        if (apiKey.tenant.id != tenantId) {
            throw NotFoundException("ApiKey", keyId)
        }
        apiKeyRepository.delete(apiKey)
    }

    // --- Helpers ---

    private fun generateSecureSecret(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun ApiKey.toDto() = ApiKeyDto(
        id = id,
        clientId = clientId,
        tenantId = tenant.id,
        name = name,
        isActive = isActive,
        permissions = permissions,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt
    )
}
