package com.hps.api.scheduling

import com.hps.common.exception.BadRequestException
import com.hps.common.exception.NotFoundException
import com.hps.domain.booking.BookingType
import com.hps.domain.scheduling.ProviderDateOverride
import com.hps.domain.scheduling.ProviderScheduleSettings
import com.hps.domain.scheduling.ProviderWeeklySlot
import com.hps.persistence.booking.BookingRepository
import com.hps.persistence.scheduling.DateOverrideRepository
import com.hps.persistence.scheduling.ScheduleSettingsRepository
import com.hps.persistence.scheduling.WeeklySlotRepository
import com.hps.persistence.service.ServiceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.*
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AvailabilityService(
    private val settingsRepository: ScheduleSettingsRepository,
    private val weeklySlotRepository: WeeklySlotRepository,
    private val overrideRepository: DateOverrideRepository,
    private val bookingRepository: BookingRepository,
    private val serviceRepository: ServiceRepository
) {
    companion object {
        private const val SLOT_MINUTES = 30L
        private const val MAX_QUERY_DAYS = 30L
    }

    fun getAvailability(
        providerId: UUID,
        from: LocalDate,
        to: LocalDate,
        serviceId: UUID
    ): AvailabilityResponse {
        if (from.isAfter(to)) throw BadRequestException("'from' must be before 'to'")
        if (from.plusDays(MAX_QUERY_DAYS).isBefore(to)) throw BadRequestException("Max range is $MAX_QUERY_DAYS days")

        val service = serviceRepository.findById(serviceId)
            .orElseThrow { NotFoundException("Service", serviceId) }
        val durationMinutes = service.durationMinutes
            ?: throw BadRequestException("Service has no duration set")

        val settings = settingsRepository.findById(providerId).orElse(null)
        val timezone = settings?.let { ZoneId.of(it.timezone) } ?: ZoneId.of("Europe/Berlin")
        val incallGap = settings?.incallGapMinutes ?: 15
        val outcallGap = settings?.outcallGapMinutes ?: 60
        val leadTimeHours = settings?.minLeadTimeHours ?: 2

        val weeklySlots = weeklySlotRepository.findByProviderUserId(providerId)
        if (weeklySlots.isEmpty() && settings == null) {
            return AvailabilityResponse(providerId, timezone.id, durationMinutes,
                generateDateRange(from, to).map { DateAvailability(it, emptyList()) })
        }

        val overrides = overrideRepository.findByProviderUserIdAndOverrideDateBetween(providerId, from, to)
        val overridesByDate = overrides.groupBy { it.overrideDate }

        // Expand query window for bookings to catch gap bleed
        val maxGap = maxOf(incallGap, outcallGap)
        val queryStart = from.atStartOfDay(timezone).toInstant().minusSeconds(maxGap * 60L)
        val queryEnd = to.plusDays(1).atStartOfDay(timezone).toInstant().plusSeconds(maxGap * 60L)
        val bookings = bookingRepository.findConfirmedByProviderAndTimeRange(providerId, queryStart, queryEnd)

        // Build blocked intervals from confirmed bookings + gaps
        data class BlockedInterval(val start: ZonedDateTime, val end: ZonedDateTime)
        val blockedIntervals = bookings.map { booking ->
            val bookingStart = booking.scheduledAt.atZone(timezone)
            val bookingDuration = booking.durationMinutes ?: durationMinutes
            val gapAfter = if (booking.bookingType == BookingType.OUTCALL) outcallGap else incallGap
            val blockEnd = bookingStart.plusMinutes(bookingDuration.toLong() + gapAfter.toLong())
            BlockedInterval(bookingStart, blockEnd)
        }

        val now = ZonedDateTime.now(timezone)
        val earliestBookable = now.plusHours(leadTimeHours.toLong())
        val slotsNeeded = ((durationMinutes + SLOT_MINUTES - 1) / SLOT_MINUTES).toInt()

        val dates = generateDateRange(from, to).map { date ->
            val rawSlots = computeRawSlots(date, weeklySlots, overridesByDate[date])
            val available = rawSlots
                .filter { slotTime ->
                    val slotStart = date.atTime(slotTime).atZone(timezone)
                    val slotEnd = slotStart.plusMinutes(SLOT_MINUTES)
                    // Filter past + lead time
                    slotStart >= earliestBookable &&
                    // Filter blocked intervals
                    blockedIntervals.none { blocked ->
                        slotStart.isBefore(blocked.end) && slotEnd.isAfter(blocked.start)
                    }
                }
                .toMutableList()

            // Filter for consecutive slots matching service duration
            val feasible = available.filter { startSlot ->
                (0 until slotsNeeded).all { offset ->
                    available.contains(startSlot.plusMinutes(offset * SLOT_MINUTES))
                }
            }

            DateAvailability(date, feasible)
        }

        return AvailabilityResponse(providerId, timezone.id, durationMinutes, dates)
    }

    private fun computeRawSlots(
        date: LocalDate,
        weeklySlots: List<ProviderWeeklySlot>,
        overrides: List<ProviderDateOverride>?
    ): List<LocalTime> {
        // If there are overrides for this date
        if (overrides != null && overrides.isNotEmpty()) {
            // If any override marks the day as unavailable, return empty
            if (overrides.any { it.isUnavailable }) return emptyList()
            // Use override time ranges
            return overrides
                .filter { it.startTime != null && it.endTime != null }
                .flatMap { expandTimeRange(it.startTime!!, it.endTime!!) }
                .distinct()
                .sorted()
        }

        // Use weekly template
        val dayOfWeek = date.dayOfWeek.value // ISO: 1=Monday
        return weeklySlots
            .filter { it.dayOfWeek == dayOfWeek }
            .flatMap { expandTimeRange(it.startTime, it.endTime) }
            .distinct()
            .sorted()
    }

    private fun expandTimeRange(start: LocalTime, end: LocalTime): List<LocalTime> {
        val slots = mutableListOf<LocalTime>()
        var current = start
        while (current.isBefore(end)) {
            slots.add(current)
            current = current.plusMinutes(SLOT_MINUTES)
        }
        return slots
    }

    private fun generateDateRange(from: LocalDate, to: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = from
        while (!current.isAfter(to)) {
            dates.add(current)
            current = current.plusDays(1)
        }
        return dates
    }
}
