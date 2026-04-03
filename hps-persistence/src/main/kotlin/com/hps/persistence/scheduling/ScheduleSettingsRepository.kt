package com.hps.persistence.scheduling

import com.hps.domain.scheduling.ProviderScheduleSettings
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ScheduleSettingsRepository : JpaRepository<ProviderScheduleSettings, UUID>
