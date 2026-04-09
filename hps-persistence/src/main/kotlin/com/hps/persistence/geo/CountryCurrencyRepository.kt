package com.hps.persistence.geo

import com.hps.domain.geo.CountryCurrency
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface CountryCurrencyRepository : JpaRepository<CountryCurrency, UUID> {

    @Query("""
        SELECT cc FROM CountryCurrency cc
        JOIN FETCH cc.country c
        JOIN FETCH c.translations
        ORDER BY c.isoCode
    """)
    fun findAllWithCountry(): List<CountryCurrency>

    fun findByCountryId(countryId: UUID): CountryCurrency?
}
