package com.hps.persistence.user

import com.hps.domain.user.ProviderApprovalHistory
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProviderApprovalHistoryRepository : JpaRepository<ProviderApprovalHistory, UUID> {

    fun findByProviderIdOrderByCreatedAtDesc(providerId: UUID): List<ProviderApprovalHistory>
}
