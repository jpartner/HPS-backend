package com.hps.api.booking

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CreateBookingRequest(
    @field:NotNull
    val providerId: UUID,
    @field:NotEmpty
    val services: List<BookingServiceRequest>,
    @field:NotNull
    val scheduledAt: Instant,
    val bookingType: String = "INCALL",
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val addressText: String? = null,
    val clientNotes: String? = null
)

data class BookingServiceRequest(
    @field:NotNull
    val serviceId: UUID,
    val quantity: Int = 1
)

data class QuoteBookingRequest(
    @field:NotNull
    val priceAmount: BigDecimal,
    val providerNotes: String? = null
)

data class UpdateStatusRequest(
    @field:NotNull
    val status: String,
    val reason: String? = null
)

data class BookingDto(
    val id: UUID,
    val clientId: UUID,
    val clientName: String?,
    val providerId: UUID,
    val providerName: String?,
    val status: String,
    val bookingType: String,
    val scheduledAt: Instant,
    val totalDurationMinutes: Int?,
    val priceAmount: BigDecimal,
    val originalAmount: BigDecimal?,
    val priceCurrency: String,
    val locationLat: Double?,
    val locationLng: Double?,
    val addressText: String?,
    val clientNotes: String?,
    val providerNotes: String?,
    val services: List<BookingServiceDto>,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class BookingServiceDto(
    val serviceId: UUID,
    val serviceTitle: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
    val durationMinutes: Int?
)

data class BookingCalculation(
    val services: List<BookingServiceDto>,
    val totalAmount: BigDecimal,
    val totalDurationMinutes: Int,
    val currency: String
)
