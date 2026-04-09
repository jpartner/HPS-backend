package com.hps.domain.geo

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "countries")
class Country(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "iso_code", nullable = false, unique = true, length = 2)
    val isoCode: String,

    @Column(name = "phone_prefix", length = 5)
    val phonePrefix: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "country", cascade = [CascadeType.ALL], orphanRemoval = true)
    val translations: MutableSet<CountryTranslation> = mutableSetOf(),

    @OneToMany(mappedBy = "country", cascade = [CascadeType.ALL], orphanRemoval = true)
    val regions: MutableList<Region> = mutableListOf(),

    @OneToOne(mappedBy = "country", cascade = [CascadeType.ALL], orphanRemoval = true)
    var currency: CountryCurrency? = null
)

@Entity
@Table(
    name = "country_translations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["country_id", "lang"])]
)
class CountryTranslation(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    val country: Country,

    @Column(name = "lang", nullable = false, length = 5)
    val lang: String,

    @Column(name = "name", nullable = false)
    val name: String
)
