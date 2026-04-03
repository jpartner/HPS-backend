package com.hps.persistence.service

import com.hps.domain.service.ServiceTemplate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ServiceTemplateRepository : JpaRepository<ServiceTemplate, UUID> {

    @Query("""
        SELECT DISTINCT t FROM ServiceTemplate t
        JOIN FETCH t.translations
        WHERE t.category.id = :categoryId
        AND t.isActive = true
        ORDER BY t.sortOrder
    """)
    fun findByCategoryWithTranslations(categoryId: UUID): List<ServiceTemplate>

    @Query("""
        SELECT DISTINCT t FROM ServiceTemplate t
        JOIN FETCH t.translations
        WHERE t.isActive = true
        ORDER BY t.sortOrder
    """)
    fun findAllActiveWithTranslations(): List<ServiceTemplate>

    fun findBySlug(slug: String): ServiceTemplate?
}
