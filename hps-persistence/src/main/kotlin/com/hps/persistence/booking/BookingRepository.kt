package com.hps.persistence.booking

import com.hps.domain.booking.Booking
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface BookingRepository : JpaRepository<Booking, UUID> {

    fun countByTenantId(tenantId: UUID): Long

    fun findByTenantId(tenantId: UUID): List<Booking>

    @Query("""
        SELECT b FROM Booking b
        WHERE b.provider.userId = :providerId
        AND b.status IN ('CONFIRMED', 'IN_PROGRESS')
        AND b.scheduledAt >= :from
        AND b.scheduledAt < :to
    """)
    fun findConfirmedByProviderAndTimeRange(
        providerId: UUID, from: Instant, to: Instant
    ): List<Booking>

    @Query("""
        SELECT b FROM Booking b
        WHERE b.client.id = :clientId
        ORDER BY b.scheduledAt DESC
    """)
    fun findByClientId(clientId: UUID): List<Booking>

    @Query("""
        SELECT b FROM Booking b
        WHERE b.provider.userId = :providerId
        ORDER BY b.scheduledAt DESC
    """)
    fun findByProviderId(providerId: UUID): List<Booking>
}
