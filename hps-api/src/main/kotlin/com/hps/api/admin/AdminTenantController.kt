package com.hps.api.admin

import com.hps.common.exception.NotFoundException
import com.hps.domain.tenant.Tenant
import com.hps.domain.tenant.TenantAdmin
import com.hps.persistence.tenant.TenantAdminRepository
import com.hps.persistence.tenant.TenantRepository
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

// --- DTOs ---

data class TenantDto(
    val id: UUID,
    val slug: String,
    val name: String,
    val domain: String?,
    val defaultLang: String,
    val supportedLangs: String,
    val defaultCurrency: String,
    val isActive: Boolean,
    val settings: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreateTenantRequest(
    val name: String,
    val slug: String,
    val defaultLang: String = "en",
    val supportedLangs: String = "en",
    val defaultCurrency: String = "EUR",
    val domain: String? = null,
    val settings: String = "{}"
)

data class UpdateTenantRequest(
    val name: String? = null,
    val slug: String? = null,
    val defaultLang: String? = null,
    val supportedLangs: String? = null,
    val defaultCurrency: String? = null,
    val domain: String? = null,
    val settings: String? = null
)

data class AssignAdminRequest(
    val userId: UUID
)

data class TenantAdminDto(
    val tenantId: UUID,
    val userId: UUID
)

// --- Controller ---

@RestController
@RequestMapping("/api/v1/admin/tenants")
class AdminTenantController(
    private val tenantRepository: TenantRepository,
    private val tenantAdminRepository: TenantAdminRepository
) {

    @GetMapping
    fun listTenants(): List<TenantDto> =
        tenantRepository.findAll().map { it.toDto() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun createTenant(@RequestBody request: CreateTenantRequest): TenantDto {
        val tenant = Tenant(
            name = request.name,
            slug = request.slug,
            defaultLang = request.defaultLang,
            supportedLangs = request.supportedLangs,
            defaultCurrency = request.defaultCurrency,
            domain = request.domain,
            settings = request.settings
        )
        return tenantRepository.save(tenant).toDto()
    }

    @GetMapping("/{id}")
    fun getTenant(@PathVariable id: UUID): TenantDto {
        val tenant = tenantRepository.findById(id)
            .orElseThrow { NotFoundException("Tenant", id) }
        return tenant.toDto()
    }

    @PutMapping("/{id}")
    @Transactional
    fun updateTenant(
        @PathVariable id: UUID,
        @RequestBody request: UpdateTenantRequest
    ): TenantDto {
        val tenant = tenantRepository.findById(id)
            .orElseThrow { NotFoundException("Tenant", id) }

        request.name?.let { tenant.name = it }
        request.slug?.let { tenant.slug = it }
        request.defaultLang?.let { tenant.defaultLang = it }
        request.supportedLangs?.let { tenant.supportedLangs = it }
        request.defaultCurrency?.let { tenant.defaultCurrency = it }
        request.domain?.let { tenant.domain = it }
        request.settings?.let { tenant.settings = it }
        tenant.updatedAt = Instant.now()

        return tenantRepository.save(tenant).toDto()
    }

    @DeleteMapping("/{id}")
    @Transactional
    fun deactivateTenant(@PathVariable id: UUID): TenantDto {
        val tenant = tenantRepository.findById(id)
            .orElseThrow { NotFoundException("Tenant", id) }
        tenant.isActive = false
        tenant.updatedAt = Instant.now()
        return tenantRepository.save(tenant).toDto()
    }

    // --- Tenant admin assignments ---

    @GetMapping("/{tenantId}/admins")
    fun listTenantAdmins(@PathVariable tenantId: UUID): List<TenantAdminDto> {
        if (!tenantRepository.existsById(tenantId)) {
            throw NotFoundException("Tenant", tenantId)
        }
        return tenantAdminRepository.findByTenantId(tenantId)
            .map { TenantAdminDto(it.tenantId, it.userId) }
    }

    @PostMapping("/{tenantId}/admins")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun assignAdmin(
        @PathVariable tenantId: UUID,
        @RequestBody request: AssignAdminRequest
    ): TenantAdminDto {
        if (!tenantRepository.existsById(tenantId)) {
            throw NotFoundException("Tenant", tenantId)
        }
        val entry = TenantAdmin(tenantId = tenantId, userId = request.userId)
        tenantAdminRepository.save(entry)
        return TenantAdminDto(tenantId, request.userId)
    }

    @DeleteMapping("/{tenantId}/admins/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun removeAdmin(
        @PathVariable tenantId: UUID,
        @PathVariable userId: UUID
    ) {
        tenantAdminRepository.deleteByTenantIdAndUserId(tenantId, userId)
    }

    // --- Mapping ---

    private fun Tenant.toDto() = TenantDto(
        id = id,
        slug = slug,
        name = name,
        domain = domain,
        defaultLang = defaultLang,
        supportedLangs = supportedLangs,
        defaultCurrency = defaultCurrency,
        isActive = isActive,
        settings = settings,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
