package com.hps.persistence.geo

import com.hps.domain.geo.Region
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface RegionRepository : JpaRepository<Region, UUID> {

    @Query("""
        SELECT r FROM Region r
        JOIN FETCH r.translations t
        WHERE r.country.isoCode = :countryCode
        AND (t.lang = :lang OR t.lang = 'en')
        ORDER BY t.name
    """)
    fun findByCountryCodeWithTranslations(countryCode: String, lang: String): List<Region>
}
