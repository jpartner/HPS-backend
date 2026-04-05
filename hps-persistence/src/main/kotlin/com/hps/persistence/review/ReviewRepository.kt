package com.hps.persistence.review

import com.hps.domain.review.Review
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ReviewRepository : JpaRepository<Review, UUID> {

    fun findByTenantId(tenantId: UUID): List<Review>
}
