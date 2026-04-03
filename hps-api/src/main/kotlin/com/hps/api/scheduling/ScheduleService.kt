package com.hps.api.scheduling

import com.hps.common.exception.BadRequestException
import com.hps.common.exception.NotFoundException
import com.hps.domain.scheduling.ProviderDateOverride
import com.hps.domain.scheduling.ProviderScheduleSettings
import com.hps.domain.scheduling.ProviderWeeklySlot
import com.hps.persistence.scheduling.DateOverrideRepository
import com.hps.persistence.scheduling.ScheduleSettingsRepository
import com.hps.persistence.scheduling.WeeklySlotRepository
import com.hps.persistence.user.ProviderProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ScheduleService(
    private val settingsRepository: ScheduleSettingsRepository,
    private val weeklySlotRepository: WeeklySlotRepository,
    private val overrideRepository: DateOverrideRepository,
    private val providerRepository: ProviderProfileRepository
) {
    fun getWeeklySchedule(providerId: UUID): WeeklyScheduleResponse {
        val settings = settingsRepository.findById(providerId).orElse(null)
        val slots = weeklySlotRepository.findByProviderUserId(providerId)

        return WeeklyScheduleResponse(
            timezone = settings?.timezone ?: "Europe/Berlin",
            incallGapMinutes = settings?.incallGapMinutes ?: 15,
            outcallGapMinutes = settings?.outcallGapMinutes ?: 60,
            minLeadTimeHours = settings?.minLeadTimeHours ?: 2,
            slots = slots.map { WeeklySlotResponse(it.dayOfWeek, it.startTime, it.endTime) }
                .sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
        )
    }

    @Transactional
    fun setWeeklySchedule(providerId: UUID, request: WeeklyScheduleRequest): WeeklyScheduleResponse {
        val provider = providerRepository.findById(providerId)
            .orElseThrow { NotFoundException("Provider", providerId) }

        // Validate timezone
        try { ZoneId.of(request.timezone) }
        catch (e: Exception) { throw BadRequestException("Invalid timezone: ${request.timezone}") }

        // Validate slots are 30-min aligned
        request.slots.forEach { slot ->
            if (slot.startTime.minute % 30 != 0 || slot.endTime.minute % 30 != 0) {
                throw BadRequestException("Slot times must be aligned to 30-minute intervals")
            }
            if (!slot.startTime.isBefore(slot.endTime)) {
                throw BadRequestException("Slot start must be before end: ${slot.startTime}-${slot.endTime}")
            }
        }

        // Upsert settings
        val settings = settingsRepository.findById(providerId)
            .orElse(ProviderScheduleSettings(provider = provider))
        settings.timezone = request.timezone
        settings.incallGapMinutes = request.incallGapMinutes
        settings.outcallGapMinutes = request.outcallGapMinutes
        settings.minLeadTimeHours = request.minLeadTimeHours
        settings.updatedAt = Instant.now()
        settingsRepository.save(settings)

        // Replace weekly slots
        weeklySlotRepository.deleteByProviderUserId(providerId)
        weeklySlotRepository.flush()

        request.slots.forEach { slot ->
            weeklySlotRepository.save(
                ProviderWeeklySlot(
                    provider = provider,
                    dayOfWeek = slot.dayOfWeek,
                    startTime = slot.startTime,
                    endTime = slot.endTime
                )
            )
        }

        return getWeeklySchedule(providerId)
    }

    fun getOverrides(providerId: UUID, from: LocalDate, to: LocalDate): List<DateOverrideResponse> {
        val overrides = overrideRepository.findByProviderUserIdAndOverrideDateBetween(providerId, from, to)
        return overrides
            .groupBy { it.overrideDate }
            .map { (date, items) ->
                val unavailable = items.any { it.isUnavailable }
                DateOverrideResponse(
                    date = date,
                    isUnavailable = unavailable,
                    timeRanges = if (unavailable) emptyList()
                        else items.filter { it.startTime != null }.map {
                            TimeRangeResponse(it.startTime!!, it.endTime!!)
                        }.sortedBy { it.startTime },
                    reason = items.firstOrNull()?.reason
                )
            }
            .sortedBy { it.date }
    }

    @Transactional
    fun setOverride(providerId: UUID, date: LocalDate, request: DateOverrideRequest): DateOverrideResponse {
        val provider = providerRepository.findById(providerId)
            .orElseThrow { NotFoundException("Provider", providerId) }

        if (date.isBefore(LocalDate.now())) {
            throw BadRequestException("Cannot set overrides for past dates")
        }

        // Delete existing overrides for this date
        overrideRepository.deleteByProviderUserIdAndOverrideDate(providerId, date)
        overrideRepository.flush()

        if (request.isUnavailable) {
            overrideRepository.save(
                ProviderDateOverride(
                    provider = provider,
                    overrideDate = date,
                    isUnavailable = true,
                    reason = request.reason
                )
            )
        } else {
            if (request.timeRanges.isEmpty()) {
                throw BadRequestException("Must provide timeRanges when not marking as unavailable")
            }
            request.timeRanges.forEach { range ->
                if (range.startTime.minute % 30 != 0 || range.endTime.minute % 30 != 0) {
                    throw BadRequestException("Times must be aligned to 30-minute intervals")
                }
                overrideRepository.save(
                    ProviderDateOverride(
                        provider = provider,
                        overrideDate = date,
                        isUnavailable = false,
                        startTime = range.startTime,
                        endTime = range.endTime,
                        reason = request.reason
                    )
                )
            }
        }

        return getOverrides(providerId, date, date).first()
    }

    @Transactional
    fun deleteOverride(providerId: UUID, date: LocalDate) {
        overrideRepository.deleteByProviderUserIdAndOverrideDate(providerId, date)
    }
}
