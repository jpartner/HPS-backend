package com.hps.domain.geo

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "cities")
class City(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    val region: Region,

    @Column(name = "latitude")
    val latitude: Double? = null,

    @Column(name = "longitude")
    val longitude: Double? = null,

    @Column(name = "population")
    val population: Int? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "city", cascade = [CascadeType.ALL], orphanRemoval = true)
    val translations: MutableSet<CityTranslation> = mutableSetOf(),

    @OneToMany(mappedBy = "city", cascade = [CascadeType.ALL], orphanRemoval = true)
    val areas: MutableList<Area> = mutableListOf()
)

@Entity
@Table(
    name = "city_translations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["city_id", "lang"])]
)
class CityTranslation(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    val city: City,

    @Column(name = "lang", nullable = false, length = 5)
    val lang: String,

    @Column(name = "name", nullable = false)
    val name: String
)
