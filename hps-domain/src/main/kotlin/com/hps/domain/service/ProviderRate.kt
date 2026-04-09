package com.hps.domain.service

import com.hps.domain.user.ProviderProfile
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "provider_rates")
class ProviderRate(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    val provider: ProviderProfile,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_type_id")
    val meetingType: ServiceCategory? = null,

    @Column(name = "duration_minutes")
    val durationMinutes: Int? = null,

    @Column(name = "label", length = 100)
    var label: String? = null,

    @Column(name = "incall_amount", precision = 10, scale = 2)
    var incallAmount: BigDecimal? = null,

    @Column(name = "outcall_amount", precision = 10, scale = 2)
    var outcallAmount: BigDecimal? = null,

    @Column(name = "secondary_incall_amount", precision = 10, scale = 2)
    var secondaryIncallAmount: BigDecimal? = null,

    @Column(name = "secondary_outcall_amount", precision = 10, scale = 2)
    var secondaryOutcallAmount: BigDecimal? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
