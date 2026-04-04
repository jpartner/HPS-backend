package com.hps.persistence.tenant

import com.hps.domain.tenant.ApiKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ApiKeyRepository : JpaRepository<ApiKey, UUID> {

    @Query("""
        SELECT k FROM ApiKey k
        JOIN FETCH k.tenant
        WHERE k.clientId = :clientId
        AND k.isActive = true
    """)
    fun findActiveByClientId(clientId: UUID): ApiKey?

    fun findByTenantId(tenantId: UUID): List<ApiKey>
}
