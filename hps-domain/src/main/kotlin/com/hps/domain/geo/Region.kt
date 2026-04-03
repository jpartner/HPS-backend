package com.hps.domain.geo

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "regions")
class Region(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    val country: Country,

    @Column(name = "code", length = 20)
    val code: String? = null,

    @Column(name = "latitude")
    val latitude: Double? = null,

    @Column(name = "longitude")
    val longitude: Double? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "region", cascade = [CascadeType.ALL], orphanRemoval = true)
    val translations: MutableSet<RegionTranslation> = mutableSetOf(),

    @OneToMany(mappedBy = "region", cascade = [CascadeType.ALL], orphanRemoval = true)
    val cities: MutableList<City> = mutableListOf()
)

@Entity
@Table(
    name = "region_translations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["region_id", "lang"])]
)
class RegionTranslation(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    val region: Region,

    @Column(name = "lang", nullable = false, length = 5)
    val lang: String,

    @Column(name = "name", nullable = false)
    val name: String
)
