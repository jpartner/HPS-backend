package com.hps.api.admin

import com.hps.common.exception.BadRequestException
import com.hps.common.exception.NotFoundException
import com.hps.common.tenant.TenantContext
import com.hps.domain.user.ApprovalStatus
import com.hps.domain.user.ProviderProfile
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
    val handle: String?,
    val cityName: String?,
    val categories: List<String>,
    val isVerified: Boolean,
    val approvalStatus: String,
    val isMobile: Boolean,
    val avgRating: BigDecimal,
    val reviewCount: Int,
    val servicesCount: Int,
    val createdAt: Instant
)

data class UpdateProviderApprovalRequest(
    val approvalStatus: String
)

// --- Controller ---

@RestController
@RequestMapping("/api/v1/admin/providers")
class AdminProviderController(
    private val providerProfileRepository: ProviderProfileRepository
) {

    @GetMapping
    @Transactional(readOnly = true)
    fun listProviders(
        @RequestParam(required = false) approvalStatus: String?
    ): List<AdminProviderDto> {
        val tenantId = TenantContext.require()
        val providers = providerProfileRepository.findByTenantId(tenantId)
        return providers
            .filter { p -> approvalStatus == null || p.approvalStatus.name == approvalStatus }
            .map { it.toDto() }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    fun getProvider(@PathVariable id: UUID): AdminProviderDto {
        val tenantId = TenantContext.require()
        val provider = providerProfileRepository.findById(id)
            .orElseThrow { NotFoundException("Provider", id) }
        if (provider.tenantId != tenantId) throw NotFoundException("Provider", id)
        return provider.toDto()
    }

    @PutMapping("/{id}/approve")
    @Transactional
    fun updateApproval(
        @PathVariable id: UUID,
        @RequestBody request: UpdateProviderApprovalRequest
    ): AdminProviderDto {
        val tenantId = TenantContext.require()
        val provider = providerProfileRepository.findById(id)
            .orElseThrow { NotFoundException("Provider", id) }
        if (provider.tenantId != tenantId) throw NotFoundException("Provider", id)

        val status = try {
            ApprovalStatus.valueOf(request.approvalStatus)
        } catch (e: IllegalArgumentException) {
            throw BadRequestException("Invalid approval status: ${request.approvalStatus}")
        }

        provider.approvalStatus = status
        provider.isVerified = status == ApprovalStatus.APPROVED
        provider.updatedAt = Instant.now()
        providerProfileRepository.save(provider)
        return provider.toDto()
    }

    // Keep old endpoint for backward compat
    @PutMapping("/{id}/verify")
    @Transactional
    fun updateVerification(
        @PathVariable id: UUID,
        @RequestBody body: Map<String, Any>
    ): AdminProviderDto {
        val isVerified = body["isVerified"] as? Boolean ?: false
        return updateApproval(id, UpdateProviderApprovalRequest(
            if (isVerified) "APPROVED" else "REJECTED"
        ))
    }

    private fun ProviderProfile.toDto() = AdminProviderDto(
        id = userId!!,
        businessName = businessName,
        email = user.email,
        handle = user.handle,
        cityName = city?.translations?.firstOrNull()?.name,
        categories = categories.mapNotNull { it.slug },
        isVerified = isVerified,
        approvalStatus = approvalStatus.name,
        isMobile = isMobile,
        avgRating = avgRating,
        reviewCount = reviewCount,
        servicesCount = services.size,
        createdAt = createdAt
    )
}
