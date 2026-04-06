package com.hps.domain.messaging

import com.hps.domain.user.User
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "message_reports",
    uniqueConstraints = [UniqueConstraint(columnNames = ["message_id", "reporter_id"])]
)
class MessageReport(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    val message: Message,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    val reporter: User,

    @Column(nullable = false, columnDefinition = "TEXT")
    val reason: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReportStatus = ReportStatus.PENDING,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    var reviewedBy: User? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null,

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    var adminNotes: String? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

enum class ReportStatus {
    PENDING, REVIEWED, DISMISSED, ACTION_TAKEN
}
