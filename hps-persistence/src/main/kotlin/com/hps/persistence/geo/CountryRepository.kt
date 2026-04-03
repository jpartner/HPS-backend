package com.hps.persistence.geo

import com.hps.domain.geo.Country
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface CountryRepository : JpaRepository<Country, UUID> {

    fun findByIsoCode(isoCode: String): Country?

    @Query("""
        SELECT c FROM Country c
        JOIN FETCH c.translations t
        WHERE t.lang = :lang OR t.lang = 'en'
        ORDER BY t.name
    """)
    fun findAllWithTranslations(lang: String): List<Country>
}
