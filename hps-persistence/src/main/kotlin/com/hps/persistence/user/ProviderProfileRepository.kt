package com.hps.persistence.user

import com.hps.domain.user.ApprovalStatus
import com.hps.domain.user.ProviderProfile
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ProviderProfileRepository : JpaRepository<ProviderProfile, UUID> {

    fun findByTenantId(tenantId: UUID): List<ProviderProfile>

    fun countByTenantId(tenantId: UUID): Long

    fun countByTenantIdAndIsVerified(tenantId: UUID, isVerified: Boolean): Long

    fun countByTenantIdAndApprovalStatus(tenantId: UUID, approvalStatus: ApprovalStatus): Long

    // Public queries: only return APPROVED providers
    @Query("""
        SELECT DISTINCT p FROM ProviderProfile p
        JOIN p.categories c
        WHERE c.id = :categoryId
        AND p.approvalStatus = 'APPROVED'
    """)
    fun findByCategoryId(categoryId: UUID, pageable: Pageable): Page<ProviderProfile>

    @Query("""
        SELECT DISTINCT p FROM ProviderProfile p
        JOIN p.categories c
        WHERE c.id = :categoryId
        AND p.city.id = :cityId
        AND p.approvalStatus = 'APPROVED'
    """)
    fun findByCategoryIdAndCityId(categoryId: UUID, cityId: UUID, pageable: Pageable): Page<ProviderProfile>

    @Query("""
        SELECT DISTINCT p FROM ProviderProfile p
        JOIN p.categories c
        JOIN p.city ci
        JOIN ci.region r
        WHERE c.id = :categoryId
        AND r.country.isoCode = :countryCode
        AND p.approvalStatus = 'APPROVED'
    """)
    fun findByCategoryIdAndCountryCode(categoryId: UUID, countryCode: String, pageable: Pageable): Page<ProviderProfile>

    @Query("""
        SELECT DISTINCT p FROM ProviderProfile p
        JOIN p.categories c
        JOIN p.city ci
        WHERE c.id = :categoryId
        AND ci.region.id = :regionId
        AND p.approvalStatus = 'APPROVED'
    """)
    fun findByCategoryIdAndRegionId(categoryId: UUID, regionId: UUID, pageable: Pageable): Page<ProviderProfile>
}
