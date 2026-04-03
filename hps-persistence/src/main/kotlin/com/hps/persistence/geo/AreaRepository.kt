package com.hps.persistence.geo

import com.hps.domain.geo.Area
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface AreaRepository : JpaRepository<Area, UUID> {

    @Query("""
        SELECT DISTINCT a FROM Area a
        JOIN FETCH a.translations
        WHERE a.city.id = :cityId
    """)
    fun findByCityIdWithTranslations(cityId: UUID, lang: String): List<Area>
}
