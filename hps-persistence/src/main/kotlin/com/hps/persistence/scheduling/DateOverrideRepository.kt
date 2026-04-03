package com.hps.persistence.scheduling

import com.hps.domain.scheduling.ProviderDateOverride
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface DateOverrideRepository : JpaRepository<ProviderDateOverride, UUID> {

    fun findByProviderUserIdAndOverrideDateBetween(
        providerId: UUID, from: LocalDate, to: LocalDate
    ): List<ProviderDateOverride>

    fun findByProviderUserIdAndOverrideDate(
        providerId: UUID, date: LocalDate
    ): List<ProviderDateOverride>

    fun deleteByProviderUserIdAndOverrideDate(providerId: UUID, date: LocalDate)
}
