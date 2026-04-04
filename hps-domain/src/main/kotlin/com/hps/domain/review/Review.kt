package com.hps.domain.review

import com.hps.domain.booking.Booking
import com.hps.domain.user.User
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "reviews",
    uniqueConstraints = [UniqueConstraint(columnNames = ["booking_id", "direction"])]
)
class Review(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    val booking: Booking,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    val reviewer: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    val reviewee: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val direction: ReviewDirection,

    @Column(nullable = false)
    val rating: Short,

    @Column(columnDefinition = "TEXT")
    var comment: String? = null,

    @Column(name = "is_visible", nullable = false)
    var isVisible: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

enum class ReviewDirection {
    CLIENT_TO_PROVIDER, PROVIDER_TO_CLIENT
}
