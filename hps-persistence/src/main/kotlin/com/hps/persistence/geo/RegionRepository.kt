package com.hps.persistence.geo

import com.hps.domain.geo.Region
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface RegionRepository : JpaRepository<Region, UUID> {

    @Query("""
        SELECT DISTINCT r FROM Region r
        JOIN FETCH r.translations
        WHERE r.country.isoCode = :countryCode
    """)
    fun findByCountryCodeWithTranslations(countryCode: String, lang: String): List<Region>
}
