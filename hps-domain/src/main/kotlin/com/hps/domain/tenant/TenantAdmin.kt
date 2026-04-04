package com.hps.domain.tenant

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "tenant_admins")
@IdClass(TenantAdminId::class)
class TenantAdmin(
    @Id
    @Column(name = "tenant_id")
    val tenantId: UUID,

    @Id
    @Column(name = "user_id")
    val userId: UUID
)

data class TenantAdminId(
    val tenantId: UUID = UUID.randomUUID(),
    val userId: UUID = UUID.randomUUID()
) : java.io.Serializable
