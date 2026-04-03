package com.hps.domain.geo

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "areas")
class Area(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    val city: City,

    @Column(name = "latitude")
    val latitude: Double? = null,

    @Column(name = "longitude")
    val longitude: Double? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "area", cascade = [CascadeType.ALL], orphanRemoval = true)
    val translations: MutableSet<AreaTranslation> = mutableSetOf()
)

@Entity
@Table(
    name = "area_translations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["area_id", "lang"])]
)
class AreaTranslation(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    val area: Area,

    @Column(name = "lang", nullable = false, length = 5)
    val lang: String,

    @Column(name = "name", nullable = false)
    val name: String
)
