package com.hps.persistence.geo

import com.hps.domain.geo.Area
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface AreaRepository : JpaRepository<Area, UUID> {

    @Query("""
        SELECT a FROM Area a
        JOIN FETCH a.translations t
        WHERE a.city.id = :cityId
        AND (t.lang = :lang OR t.lang = 'en')
        ORDER BY t.name
    """)
    fun findByCityIdWithTranslations(cityId: UUID, lang: String): List<Area>
}
