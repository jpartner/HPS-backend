package com.hps.api.scheduling

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

// === Weekly Schedule ===

data class WeeklyScheduleRequest(
    val timezone: String = "Europe/Berlin",
    @field:Min(0) @field:Max(120)
    val incallGapMinutes: Int = 15,
    @field:Min(0) @field:Max(180)
    val outcallGapMinutes: Int = 60,
    @field:Min(0) @field:Max(48)
    val minLeadTimeHours: Int = 2,
    val slots: List<WeeklySlotRequest> = emptyList()
)

data class WeeklySlotRequest(
    @field:Min(1) @field:Max(7)
    val dayOfWeek: Int,
    val startTime: LocalTime,
    val endTime: LocalTime
)

data class WeeklyScheduleResponse(
    val timezone: String,
    val incallGapMinutes: Int,
    val outcallGapMinutes: Int,
    val minLeadTimeHours: Int,
    val slots: List<WeeklySlotResponse>
)

data class WeeklySlotResponse(
    val dayOfWeek: Int,
    val startTime: LocalTime,
    val endTime: LocalTime
)

// === Date Overrides ===

data class DateOverrideRequest(
    val isUnavailable: Boolean = false,
    val timeRanges: List<TimeRangeRequest> = emptyList(),
    val reason: String? = null
)

data class TimeRangeRequest(
    val startTime: LocalTime,
    val endTime: LocalTime
)

data class DateOverrideResponse(
    val date: LocalDate,
    val isUnavailable: Boolean,
    val timeRanges: List<TimeRangeResponse>,
    val reason: String?
)

data class TimeRangeResponse(
    val startTime: LocalTime,
    val endTime: LocalTime
)

// === Availability ===

data class AvailabilityResponse(
    val providerId: UUID,
    val timezone: String,
    val serviceDurationMinutes: Int,
    val dates: List<DateAvailability>
)

data class DateAvailability(
    val date: LocalDate,
    val slots: List<LocalTime>
)
