package com.hps.persistence.service

import com.hps.domain.service.Service
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ServiceRepository : JpaRepository<Service, UUID> {

    @Query("""
        SELECT s FROM Service s
        JOIN FETCH s.translations t
        JOIN FETCH s.category
        WHERE s.provider.userId = :providerId
        AND s.isActive = true
        AND (t.lang = :lang OR t.lang = 'en')
        ORDER BY s.createdAt DESC
    """)
    fun findByProviderWithTranslations(providerId: UUID, lang: String): List<Service>

    @Query("""
        SELECT s FROM Service s
        JOIN FETCH s.translations t
        JOIN FETCH s.category c
        JOIN FETCH s.provider p
        WHERE s.id = :id
        AND (t.lang = :lang OR t.lang = 'en')
    """)
    fun findByIdWithTranslations(id: UUID, lang: String): Service?

    @Query("""
        SELECT s FROM Service s
        JOIN FETCH s.translations t
        JOIN FETCH s.category c
        WHERE s.id IN :ids
        AND s.isActive = true
        AND (t.lang = :lang OR t.lang = 'en')
    """)
    fun findActiveByIdsWithTranslations(ids: List<UUID>, lang: String): List<Service>
}
