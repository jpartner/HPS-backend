package com.hps.api.admin

import com.hps.common.exception.NotFoundException
import com.hps.common.tenant.TenantContext
import com.hps.persistence.user.ProviderProfileRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// --- DTOs ---

data class AdminProviderDto(
    val id: UUID,
    val businessName: String?,
    val email: String,
    val cityName: String?,
    val categories: List<String>,
    val isVerified: Boolean,
    val isMobile: Boolean,
    val avgRating: BigDecimal,
    val reviewCount: Int,
    val servicesCount: Int,
    val createdAt: Instant
)

data class UpdateProviderVerificationRequest(
    val isVerified: Boolean
)

// --- Controller ---

@RestController
@RequestMapping("/api/v1/admin/providers")
class AdminProviderController(
    private val providerProfileRepository: ProviderProfileRepository
) {

    @GetMapping
    @Transactional(readOnly = true)
    fun listProviders(): List<AdminProviderDto> {
        val tenantId = TenantContext.require()
        val providers = providerProfileRepository.findByTenantId(tenantId)
        return providers.map { provider ->
            AdminProviderDto(
                id = provider.userId!!,
                businessName = provider.businessName,
                email = provider.user.email,
                cityName = provider.city?.translations?.firstOrNull()?.name,
                categories = provider.categories.mapNotNull { it.slug },
                isVerified = provider.isVerified,
                isMobile = provider.isMobile,
                avgRating = provider.avgRating,
                reviewCount = provider.reviewCount,
                servicesCount = provider.services.size,
                createdAt = provider.createdAt
            )
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    fun getProvider(@PathVariable id: UUID): AdminProviderDto {
        val tenantId = TenantContext.require()
        val provider = providerProfileRepository.findById(id)
            .orElseThrow { NotFoundException("Provider", id) }
        if (provider.tenantId != tenantId) {
            throw NotFoundException("Provider", id)
        }
        return AdminProviderDto(
            id = provider.userId!!,
            businessName = provider.businessName,
            email = provider.user.email,
            cityName = provider.city?.translations?.firstOrNull()?.name,
            categories = provider.categories.mapNotNull { it.slug },
            isVerified = provider.isVerified,
            isMobile = provider.isMobile,
            avgRating = provider.avgRating,
            reviewCount = provider.reviewCount,
            servicesCount = provider.services.size,
            createdAt = provider.createdAt
        )
    }

    @PutMapping("/{id}/verify")
    @Transactional
    fun updateProviderVerification(
        @PathVariable id: UUID,
        @RequestBody request: UpdateProviderVerificationRequest
    ): AdminProviderDto {
        val tenantId = TenantContext.require()
        val provider = providerProfileRepository.findById(id)
            .orElseThrow { NotFoundException("Provider", id) }
        if (provider.tenantId != tenantId) {
            throw NotFoundException("Provider", id)
        }
        provider.isVerified = request.isVerified
        provider.updatedAt = Instant.now()
        val saved = providerProfileRepository.save(provider)
        return AdminProviderDto(
            id = saved.userId!!,
            businessName = saved.businessName,
            email = saved.user.email,
            cityName = saved.city?.translations?.firstOrNull()?.name,
            categories = saved.categories.mapNotNull { it.slug },
            isVerified = saved.isVerified,
            isMobile = saved.isMobile,
            avgRating = saved.avgRating,
            reviewCount = saved.reviewCount,
            servicesCount = saved.services.size,
            createdAt = saved.createdAt
        )
    }
}
