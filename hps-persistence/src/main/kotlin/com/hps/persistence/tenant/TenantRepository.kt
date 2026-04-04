package com.hps.persistence.tenant

import com.hps.domain.tenant.Tenant
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TenantRepository : JpaRepository<Tenant, UUID> {

    fun findBySlug(slug: String): Tenant?

    fun findByIsActiveTrue(): List<Tenant>
}
