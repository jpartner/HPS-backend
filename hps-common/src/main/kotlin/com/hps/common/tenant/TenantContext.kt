package com.hps.common.tenant

import java.util.UUID

object TenantContext {
    private val currentTenant = ThreadLocal<UUID?>()

    fun get(): UUID? = currentTenant.get()

    fun require(): UUID = currentTenant.get()
        ?: throw IllegalStateException("No tenant context set")

    fun set(tenantId: UUID?) {
        currentTenant.set(tenantId)
    }

    fun clear() {
        currentTenant.remove()
    }
}
