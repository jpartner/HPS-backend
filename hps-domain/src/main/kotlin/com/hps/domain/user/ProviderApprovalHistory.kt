package com.hps.domain.user

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "provider_approval_history")
class ProviderApprovalHistory(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "provider_id", nullable = false)
    val providerId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: ApprovalStatus,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    val changedBy: User,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
