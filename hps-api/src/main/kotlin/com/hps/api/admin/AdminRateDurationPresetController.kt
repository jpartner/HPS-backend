package com.hps.api.admin

import com.hps.common.exception.NotFoundException
import com.hps.common.tenant.TenantContext
import com.hps.domain.service.RateDurationPreset
import com.hps.persistence.service.RateDurationPresetRepository
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class RateDurationPresetDto(
    val id: UUID,
    val durationMinutes: Int,
    val label: String?,
    val sortOrder: Int,
    val isActive: Boolean
)

data class CreateRateDurationPresetRequest(
    val durationMinutes: Int,
    val label: String? = null,
    val sortOrder: Int = 0
)

data class UpdateRateDurationPresetRequest(
    val sortOrder: Int? = null,
    val label: String? = null,
    val isActive: Boolean? = null
)

@RestController
@RequestMapping("/api/v1/admin/rate-duration-presets")
class AdminRateDurationPresetController(
    private val presetRepository: RateDurationPresetRepository
) {

    @GetMapping
    @Transactional(readOnly = true)
    fun list(): List<RateDurationPresetDto> {
        val tenantId = TenantContext.require()
        return presetRepository.findByTenantIdOrderBySortOrder(tenantId).map { it.toDto() }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun create(@RequestBody request: CreateRateDurationPresetRequest): RateDurationPresetDto {
        val tenantId = TenantContext.require()
        val preset = RateDurationPreset(
            tenantId = tenantId,
            durationMinutes = request.durationMinutes,
            label = request.label,
            sortOrder = request.sortOrder
        )
        return presetRepository.save(preset).toDto()
    }

    @PutMapping("/{id}")
    @Transactional
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdateRateDurationPresetRequest
    ): RateDurationPresetDto {
        val tenantId = TenantContext.require()
        val preset = presetRepository.findById(id)
            .orElseThrow { NotFoundException("RateDurationPreset", id) }
        if (preset.tenantId != tenantId) throw NotFoundException("RateDurationPreset", id)

        request.sortOrder?.let { preset.sortOrder = it }
        request.label?.let { preset.label = it }
        request.isActive?.let { preset.isActive = it }

        return presetRepository.save(preset).toDto()
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun delete(@PathVariable id: UUID) {
        val tenantId = TenantContext.require()
        val preset = presetRepository.findById(id)
            .orElseThrow { NotFoundException("RateDurationPreset", id) }
        if (preset.tenantId != tenantId) throw NotFoundException("RateDurationPreset", id)

        preset.isActive = false
        presetRepository.save(preset)
    }

    private fun RateDurationPreset.toDto() = RateDurationPresetDto(
        id = id,
        durationMinutes = durationMinutes,
        label = label,
        sortOrder = sortOrder,
        isActive = isActive
    )
}
