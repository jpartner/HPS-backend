package com.hps.api.admin

import com.hps.api.auth.userId
import com.hps.common.exception.BadRequestException
import com.hps.common.exception.NotFoundException
import com.hps.common.tenant.TenantContext
import com.hps.domain.user.ApprovalStatus
import com.hps.domain.user.ProviderApprovalHistory
import com.hps.domain.user.ProviderProfile
import com.hps.persistence.user.ProviderApprovalHistoryRepository
import com.hps.persistence.user.ProviderProfileRepository
import com.hps.persistence.user.UserRepository
import org.springframework.security.core.Authentication
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
    val approvalNotes: String?,
    val isMobile: Boolean,
    val avgRating: BigDecimal,
    val reviewCount: Int,
    val servicesCount: Int,
    val createdAt: Instant
)

data class UpdateProviderApprovalRequest(
    val approvalStatus: String,
    val notes: String? = null
)

data class ApprovalHistoryDto(
    val id: UUID,
    val status: String,
    val notes: String?,
    val changedByEmail: String,
    val createdAt: Instant
)

// --- Controller ---

@RestController
@RequestMapping("/api/v1/admin/providers")
class AdminProviderController(
    private val providerProfileRepository: ProviderProfileRepository,
    private val approvalHistoryRepository: ProviderApprovalHistoryRepository,
    private val userRepository: UserRepository
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

    @GetMapping("/{id}/history")
    @Transactional(readOnly = true)
    fun getApprovalHistory(@PathVariable id: UUID): List<ApprovalHistoryDto> {
        val tenantId = TenantContext.require()
        val provider = providerProfileRepository.findById(id)
            .orElseThrow { NotFoundException("Provider", id) }
        if (provider.tenantId != tenantId) throw NotFoundException("Provider", id)

        return approvalHistoryRepository.findByProviderIdOrderByCreatedAtDesc(id).map {
            ApprovalHistoryDto(
                id = it.id,
                status = it.status.name,
                notes = it.notes,
                changedByEmail = it.changedBy.email,
                createdAt = it.createdAt
            )
        }
    }

    @PutMapping("/{id}/approve")
    @Transactional
    fun updateApproval(
        @PathVariable id: UUID,
        @RequestBody request: UpdateProviderApprovalRequest,
        auth: Authentication
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

        val adminUser = userRepository.findById(auth.userId())
            .orElseThrow { NotFoundException("User", auth.userId()) }

        // Log history
        approvalHistoryRepository.save(
            ProviderApprovalHistory(
                providerId = provider.userId!!,
                status = status,
                notes = request.notes,
                changedBy = adminUser
            )
        )

        provider.approvalStatus = status
        provider.isVerified = status == ApprovalStatus.APPROVED
        provider.approvalNotes = request.notes
        provider.updatedAt = Instant.now()
        providerProfileRepository.save(provider)
        return provider.toDto()
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
        approvalNotes = approvalNotes,
        isMobile = isMobile,
        avgRating = avgRating,
        reviewCount = reviewCount,
        servicesCount = services.size,
        createdAt = createdAt
    )
}
