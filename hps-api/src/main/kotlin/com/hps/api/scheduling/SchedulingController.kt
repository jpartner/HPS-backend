package com.hps.api.scheduling

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class SchedulingController(
    private val scheduleService: ScheduleService,
    private val availabilityService: AvailabilityService
) {
    // === Provider Schedule Management ===

    @GetMapping("/providers/me/schedule/weekly")
    fun getWeeklySchedule(auth: Authentication): WeeklyScheduleResponse {
        val userId = UUID.fromString(auth.principal as String)
        return scheduleService.getWeeklySchedule(userId)
    }

    @PutMapping("/providers/me/schedule/weekly")
    fun setWeeklySchedule(
        @Valid @RequestBody request: WeeklyScheduleRequest,
        auth: Authentication
    ): WeeklyScheduleResponse {
        val userId = UUID.fromString(auth.principal as String)
        return scheduleService.setWeeklySchedule(userId, request)
    }

    @GetMapping("/providers/me/schedule/overrides")
    fun getOverrides(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        auth: Authentication
    ): List<DateOverrideResponse> {
        val userId = UUID.fromString(auth.principal as String)
        return scheduleService.getOverrides(userId, from, to)
    }

    @PutMapping("/providers/me/schedule/overrides/{date}")
    fun setOverride(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @Valid @RequestBody request: DateOverrideRequest,
        auth: Authentication
    ): DateOverrideResponse {
        val userId = UUID.fromString(auth.principal as String)
        return scheduleService.setOverride(userId, date, request)
    }

    @DeleteMapping("/providers/me/schedule/overrides/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteOverride(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        auth: Authentication
    ) {
        val userId = UUID.fromString(auth.principal as String)
        scheduleService.deleteOverride(userId, date)
    }

    // === Public Availability Query ===

    @GetMapping("/providers/{providerId}/availability")
    fun getAvailability(
        @PathVariable providerId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam serviceId: UUID
    ): AvailabilityResponse {
        return availabilityService.getAvailability(providerId, from, to, serviceId)
    }
}
