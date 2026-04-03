package com.hps.domain.scheduling

import com.hps.domain.user.ProviderProfile
import jakarta.persistence.*
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "provider_weekly_slots")
class ProviderWeeklySlot(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    val provider: ProviderProfile,

    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: Int,

    @Column(name = "start_time", nullable = false)
    val startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    val endTime: LocalTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
