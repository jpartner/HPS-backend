package com.hps.domain.scheduling

import com.hps.domain.user.ProviderProfile
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "provider_schedule_settings")
class ProviderScheduleSettings(
    @Id
    var providerId: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "provider_id")
    val provider: ProviderProfile,

    @Column(nullable = false, length = 50)
    var timezone: String = "Europe/Berlin",

    @Column(name = "incall_gap_minutes", nullable = false)
    var incallGapMinutes: Int = 15,

    @Column(name = "outcall_gap_minutes", nullable = false)
    var outcallGapMinutes: Int = 60,

    @Column(name = "min_lead_time_hours", nullable = false)
    var minLeadTimeHours: Int = 2,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
