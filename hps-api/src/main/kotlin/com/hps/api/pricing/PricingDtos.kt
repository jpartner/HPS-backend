package com.hps.api.pricing

import java.math.BigDecimal
import java.util.UUID

data class PricingCalculateRequest(
    val providerId: UUID,
    val rateType: String,
    val durationMinutes: Int?,
    val extraIds: List<UUID> = emptyList()
)

data class PricingBreakdownDto(
    val primaryCurrency: String,
    val secondaryCurrency: String?,
    val baseRate: PricingLineDto?,
    val extras: List<PricingExtraLineDto>,
    val totalPrimaryAmount: BigDecimal,
    val totalSecondaryAmount: BigDecimal?
)

data class PricingLineDto(
    val rateType: String,
    val durationMinutes: Int?,
    val primaryAmount: BigDecimal,
    val secondaryAmount: BigDecimal?
)

data class PricingExtraLineDto(
    val extraId: UUID,
    val name: String,
    val isIncluded: Boolean,
    val primaryAmount: BigDecimal,
    val secondaryAmount: BigDecimal?
)
