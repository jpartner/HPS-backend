package com.hps.persistence.geo

import com.hps.domain.geo.City
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface CityRepository : JpaRepository<City, UUID> {

    @Query("""
        SELECT c FROM City c
        JOIN FETCH c.translations t
        WHERE c.region.id = :regionId
        AND (t.lang = :lang OR t.lang = 'en')
        ORDER BY t.name
    """)
    fun findByRegionIdWithTranslations(regionId: UUID, lang: String): List<City>
}
