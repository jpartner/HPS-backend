package com.hps.persistence.user

import com.hps.domain.user.ProviderGalleryImage
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProviderGalleryImageRepository : JpaRepository<ProviderGalleryImage, UUID> {

    fun findByProviderUserIdOrderBySortOrder(providerId: UUID): List<ProviderGalleryImage>

    fun countByProviderUserId(providerId: UUID): Int
}
