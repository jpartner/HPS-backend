package com.hps.persistence.tenant

import com.hps.domain.tenant.TenantAdmin
import com.hps.domain.tenant.TenantAdminId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TenantAdminRepository : JpaRepository<TenantAdmin, TenantAdminId> {

    fun findByTenantId(tenantId: UUID): List<TenantAdmin>

    fun deleteByTenantIdAndUserId(tenantId: UUID, userId: UUID)
}
