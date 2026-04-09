package com.hps.domain.geo

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "country_currencies")
class CountryCurrency(
    @Id
    @Column(name = "country_id")
    var countryId: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "country_id")
    val country: Country,

    @Column(name = "primary_currency", nullable = false, length = 3)
    var primaryCurrency: String,

    @Column(name = "secondary_currency", length = 3)
    var secondaryCurrency: String? = null
)
