package com.hps.persistence.service

import com.hps.domain.service.ServiceCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ServiceCategoryRepository : JpaRepository<ServiceCategory, UUID> {

    @Query("""
        SELECT DISTINCT c FROM ServiceCategory c
        JOIN FETCH c.translations
        ORDER BY c.sortOrder
    """)
    fun findAllWithTranslations(): List<ServiceCategory>

    @Query("""
        SELECT c FROM ServiceCategory c
        JOIN FETCH c.translations
        WHERE c.slug = :slug
    """)
    fun findBySlug(slug: String): ServiceCategory?
}
