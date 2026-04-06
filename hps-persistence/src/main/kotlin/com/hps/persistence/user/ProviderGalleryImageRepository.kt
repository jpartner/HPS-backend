package com.hps.persistence.user

import com.hps.domain.user.MediaApprovalStatus
import com.hps.domain.user.MediaType
import com.hps.domain.user.ProviderMedia
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ProviderMediaRepository : JpaRepository<ProviderMedia, UUID> {

    fun findByProviderUserIdOrderBySortOrder(providerId: UUID): List<ProviderMedia>

    fun findByProviderUserIdAndMediaTypeOrderBySortOrder(
        providerId: UUID, mediaType: MediaType
    ): List<ProviderMedia>

    fun findByProviderUserIdAndMediaTypeAndApprovalStatusAndIsPrivateOrderBySortOrder(
        providerId: UUID, mediaType: MediaType, approvalStatus: MediaApprovalStatus, isPrivate: Boolean
    ): List<ProviderMedia>

    fun countByProviderUserId(providerId: UUID): Int

    fun countByProviderUserIdAndMediaType(providerId: UUID, mediaType: MediaType): Int

    @Query("""
        SELECT m FROM ProviderMedia m
        JOIN m.provider p
        WHERE p.tenantId = :tenantId
        AND m.approvalStatus = :status
        ORDER BY m.createdAt DESC
    """)
    fun findByTenantIdAndApprovalStatus(tenantId: UUID, status: MediaApprovalStatus): List<ProviderMedia>
}

// Keep old name for backward compat
@Suppress("unused")
typealias ProviderGalleryImageRepository = ProviderMediaRepository
