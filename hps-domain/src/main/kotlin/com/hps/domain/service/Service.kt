package com.hps.domain.service

import com.hps.domain.user.ProviderProfile
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "services")
class Service(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    val provider: ProviderProfile,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: ServiceCategory,

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false)
    var pricingType: PricingType,

    @Column(name = "price_amount", nullable = false, precision = 10, scale = 2)
    var priceAmount: BigDecimal,

    @Column(name = "price_currency", nullable = false, length = 3)
    var priceCurrency: String = "EUR",

    @Column(name = "duration_minutes")
    var durationMinutes: Int? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "service", cascade = [CascadeType.ALL], orphanRemoval = true)
    val translations: MutableSet<ServiceTranslation> = mutableSetOf(),

    @OneToMany(mappedBy = "service", cascade = [CascadeType.ALL], orphanRemoval = true)
    val images: MutableList<ServiceImage> = mutableListOf()
)

enum class PricingType {
    FIXED, HOURLY
}

@Entity
@Table(
    name = "service_translations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["service_id", "lang"])]
)
class ServiceTranslation(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    val service: Service,

    @Column(name = "lang", nullable = false, length = 5)
    val lang: String,

    @Column(name = "title", nullable = false)
    val title: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null
)

@Entity
@Table(name = "service_images")
class ServiceImage(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    val service: Service,

    @Column(nullable = false, length = 500)
    val url: String,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
