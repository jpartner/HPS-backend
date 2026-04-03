package com.hps.api.service

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.util.UUID

data class ServiceDto(
    val id: UUID,
    val title: String,
    val description: String?,
    val categoryId: UUID,
    val categoryName: String,
    val pricingType: String,
    val priceAmount: BigDecimal,
    val priceCurrency: String,
    val durationMinutes: Int?,
    val isActive: Boolean,
    val providerId: UUID,
    val providerName: String?
)

data class CreateServiceRequest(
    @field:NotNull
    val categoryId: UUID,

    val pricingType: String = "FIXED",

    @field:NotNull
    @field:Positive
    val priceAmount: BigDecimal,

    val priceCurrency: String = "EUR",

    val durationMinutes: Int? = null,

    val translations: List<ServiceTranslationRequest> = emptyList()
)

data class UpdateServiceRequest(
    val categoryId: UUID? = null,
    val pricingType: String? = null,
    val priceAmount: BigDecimal? = null,
    val priceCurrency: String? = null,
    val durationMinutes: Int? = null,
    val isActive: Boolean? = null,
    val translations: List<ServiceTranslationRequest>? = null
)

data class ServiceTranslationRequest(
    val lang: String,
    val title: String,
    val description: String? = null
)
