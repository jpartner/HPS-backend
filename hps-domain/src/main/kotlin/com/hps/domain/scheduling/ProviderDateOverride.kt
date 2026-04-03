package com.hps.domain.scheduling

import com.hps.domain.user.ProviderProfile
import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "provider_date_overrides")
class ProviderDateOverride(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    val provider: ProviderProfile,

    @Column(name = "override_date", nullable = false)
    val overrideDate: LocalDate,

    @Column(name = "is_unavailable", nullable = false)
    val isUnavailable: Boolean = false,

    @Column(name = "start_time")
    val startTime: LocalTime? = null,

    @Column(name = "end_time")
    val endTime: LocalTime? = null,

    @Column(length = 255)
    val reason: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
