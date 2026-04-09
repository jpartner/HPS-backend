package com.hps.persistence.service

import com.hps.domain.service.ProviderRate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ProviderRateRepository : JpaRepository<ProviderRate, UUID> {

    @Query("""
        SELECT r FROM ProviderRate r
        LEFT JOIN FETCH r.meetingType
        WHERE r.provider.userId = :providerId
        AND r.isActive = true
        ORDER BY r.meetingType.id, r.sortOrder
    """)
    fun findByProviderIdAndIsActiveTrue(providerId: UUID): List<ProviderRate>

    fun findByProviderUserId(providerId: UUID): List<ProviderRate>

    @Modifying
    @Query("DELETE FROM ProviderRate r WHERE r.provider.userId = :providerId")
    fun deleteByProviderId(providerId: UUID)
}
