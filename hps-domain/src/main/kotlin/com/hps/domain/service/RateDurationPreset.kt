package com.hps.domain.service

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "rate_duration_presets")
class RateDurationPreset(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "duration_minutes", nullable = false)
    var durationMinutes: Int,

    @Column(name = "label", length = 100)
    var label: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
)
