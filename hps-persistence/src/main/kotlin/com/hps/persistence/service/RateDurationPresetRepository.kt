package com.hps.persistence.service

import com.hps.domain.service.RateDurationPreset
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RateDurationPresetRepository : JpaRepository<RateDurationPreset, UUID> {

    fun findByTenantIdAndIsActiveTrueOrderBySortOrder(tenantId: UUID): List<RateDurationPreset>

    fun findByTenantIdOrderBySortOrder(tenantId: UUID): List<RateDurationPreset>
}
