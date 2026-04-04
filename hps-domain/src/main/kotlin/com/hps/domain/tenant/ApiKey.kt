package com.hps.domain.tenant

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "api_keys")
class ApiKey(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "client_id", nullable = false, unique = true)
    val clientId: UUID,

    @Column(name = "client_secret_hash", nullable = false)
    val clientSecretHash: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    val tenant: Tenant,

    @Column(nullable = false)
    var name: String,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(columnDefinition = "JSONB", nullable = false)
    var permissions: String = "{}",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null
)
