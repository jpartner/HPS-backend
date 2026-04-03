package com.hps.persistence.attribute

import com.hps.domain.attribute.AttributeDefinition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface AttributeDefinitionRepository : JpaRepository<AttributeDefinition, UUID> {

    @Query("""
        SELECT DISTINCT a FROM AttributeDefinition a
        JOIN FETCH a.translations
        WHERE a.domain = :domain
        AND a.isActive = true
        ORDER BY a.sortOrder
    """)
    fun findByDomainWithTranslations(domain: String): List<AttributeDefinition>

    @Query("""
        SELECT DISTINCT a FROM AttributeDefinition a
        JOIN FETCH a.translations
        WHERE a.isActive = true
        ORDER BY a.domain, a.sortOrder
    """)
    fun findAllActiveWithTranslations(): List<AttributeDefinition>
}
