package com.hps.domain.user

import com.hps.domain.geo.Area
import com.hps.domain.geo.City
import com.hps.domain.service.Service
import com.hps.domain.service.ServiceCategory
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "provider_profiles")
class ProviderProfile(
    @Id
    var userId: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    val user: User,

    @Column(name = "business_name")
    var businessName: String? = null,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    var city: City? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    var area: Area? = null,

    @Column(name = "address_line", length = 500)
    var addressLine: String? = null,

    @Column(name = "latitude")
    var latitude: Double? = null,

    @Column(name = "longitude")
    var longitude: Double? = null,

    @Column(name = "service_radius_km", precision = 6, scale = 2)
    var serviceRadiusKm: BigDecimal? = null,

    @Column(name = "is_mobile", nullable = false)
    var isMobile: Boolean = false,

    @Column(name = "is_verified", nullable = false)
    var isVerified: Boolean = false,

    @Column(name = "avg_rating", precision = 3, scale = 2)
    var avgRating: BigDecimal = BigDecimal.ZERO,

    @Column(name = "review_count")
    var reviewCount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @ManyToMany
    @JoinTable(
        name = "provider_categories",
        joinColumns = [JoinColumn(name = "provider_id")],
        inverseJoinColumns = [JoinColumn(name = "category_id")]
    )
    val categories: MutableSet<ServiceCategory> = mutableSetOf(),

    @OneToMany(mappedBy = "provider", cascade = [CascadeType.ALL], orphanRemoval = true)
    val services: MutableList<Service> = mutableListOf()
)
