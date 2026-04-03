package com.hps.domain.booking

import com.hps.domain.service.Service
import com.hps.domain.user.ProviderProfile
import com.hps.domain.user.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "bookings")
class Booking(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    val service: Service,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    val client: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    val provider: ProviderProfile,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: BookingStatus = BookingStatus.REQUESTED,

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", nullable = false)
    var bookingType: BookingType = BookingType.INCALL,

    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: Instant,

    @Column(name = "duration_minutes")
    var durationMinutes: Int? = null,

    @Column(name = "price_amount", nullable = false, precision = 10, scale = 2)
    val priceAmount: BigDecimal,

    @Column(name = "price_currency", nullable = false, length = 3)
    val priceCurrency: String,

    @Column(name = "location_lat")
    var locationLat: Double? = null,

    @Column(name = "location_lng")
    var locationLng: Double? = null,

    @Column(name = "address_text", length = 500)
    var addressText: String? = null,

    @Column(name = "client_notes", columnDefinition = "TEXT")
    var clientNotes: String? = null,

    @Column(name = "provider_notes", columnDefinition = "TEXT")
    var providerNotes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

enum class BookingType {
    INCALL, OUTCALL
}

enum class BookingStatus {
    REQUESTED, CONFIRMED, IN_PROGRESS,
    COMPLETED, CANCELLED_BY_CLIENT, CANCELLED_BY_PROVIDER,
    NO_SHOW
}

@Entity
@Table(name = "booking_status_history")
class BookingStatusHistory(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    val booking: Booking,

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    val oldStatus: BookingStatus? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    val newStatus: BookingStatus,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    val changedBy: User,

    @Column(columnDefinition = "TEXT")
    val reason: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
