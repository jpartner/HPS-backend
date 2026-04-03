package com.hps.persistence.geo

import com.hps.domain.geo.City
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface CityRepository : JpaRepository<City, UUID> {

    @Query("""
        SELECT DISTINCT c FROM City c
        JOIN FETCH c.translations
        WHERE c.region.id = :regionId
    """)
    fun findByRegionIdWithTranslations(regionId: UUID, lang: String): List<City>
}
