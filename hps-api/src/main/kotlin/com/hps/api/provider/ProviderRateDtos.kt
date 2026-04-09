package com.hps.api.provider

import java.math.BigDecimal
import java.util.UUID

data class ProviderRateCardDto(
    val primaryCurrency: String,
    val secondaryCurrency: String?,
    val meetingTypes: List<MeetingTypeRatesDto>
)

data class MeetingTypeRatesDto(
    val meetingTypeId: UUID,
    val meetingTypeName: String,
    val rates: List<RateRowDto>
)

data class RateRowDto(
    val durationMinutes: Int?,
    val label: String?,
    val incallAmount: BigDecimal?,
    val outcallAmount: BigDecimal?,
    val secondaryIncallAmount: BigDecimal?,
    val secondaryOutcallAmount: BigDecimal?,
    val sortOrder: Int = 0
)

data class UpdateRateCardRequest(
    val meetingTypes: List<MeetingTypeRatesInput> = emptyList()
)

data class MeetingTypeRatesInput(
    val meetingTypeId: UUID,
    val rates: List<RateRowInput> = emptyList()
)

data class RateRowInput(
    val durationMinutes: Int? = null,
    val label: String? = null,
    val incallAmount: BigDecimal? = null,
    val outcallAmount: BigDecimal? = null,
    val secondaryIncallAmount: BigDecimal? = null,
    val secondaryOutcallAmount: BigDecimal? = null,
    val sortOrder: Int = 0
)
