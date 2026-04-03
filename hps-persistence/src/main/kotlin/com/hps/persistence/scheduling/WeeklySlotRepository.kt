package com.hps.persistence.scheduling

import com.hps.domain.scheduling.ProviderWeeklySlot
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface WeeklySlotRepository : JpaRepository<ProviderWeeklySlot, UUID> {

    fun findByProviderUserId(providerId: UUID): List<ProviderWeeklySlot>

    fun deleteByProviderUserId(providerId: UUID)
}
