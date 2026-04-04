package com.hps.api.admin

import com.hps.common.tenant.TenantContext
import com.hps.persistence.booking.BookingRepository
import com.hps.persistence.user.ProviderProfileRepository
import com.hps.persistence.user.UserRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// --- DTOs ---

data class DashboardStatsDto(
    val totalUsers: Long,
    val totalProviders: Long,
    val totalBookings: Long,
    val verifiedProviders: Long
)

// --- Controller ---

@RestController
@RequestMapping("/api/v1/admin/dashboard")
class AdminDashboardController(
    private val userRepository: UserRepository,
    private val providerProfileRepository: ProviderProfileRepository,
    private val bookingRepository: BookingRepository
) {

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    fun getStats(): DashboardStatsDto {
        val tenantId = TenantContext.require()
        return DashboardStatsDto(
            totalUsers = userRepository.countByTenantId(tenantId),
            totalProviders = providerProfileRepository.countByTenantId(tenantId),
            totalBookings = bookingRepository.countByTenantId(tenantId),
            verifiedProviders = providerProfileRepository.countByTenantIdAndIsVerified(tenantId, true)
        )
    }
}
