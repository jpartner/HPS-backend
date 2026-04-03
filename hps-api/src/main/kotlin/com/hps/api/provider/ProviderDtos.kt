package com.hps.api.provider

import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.util.UUID

data class ProviderSummaryDto(
    val id: UUID,
    val businessName: String?,
    val description: String?,
    val cityName: String?,
    val areaName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isMobile: Boolean,
    val isVerified: Boolean,
    val avgRating: BigDecimal,
    val reviewCount: Int,
    val categories: List<ProviderCategoryDto>,
    val avatarUrl: String? = null
)

data class ProviderCategoryDto(
    val id: UUID,
    val name: String
)

data class ProviderDetailDto(
    val id: UUID,
    val businessName: String?,
    val description: String?,
    val email: String,
    val phone: String?,
    val cityName: String?,
    val areaName: String?,
    val addressLine: String?,
    val latitude: Double?,
    val longitude: Double?,
    val serviceRadiusKm: BigDecimal?,
    val isMobile: Boolean,
    val isVerified: Boolean,
    val avgRating: BigDecimal,
    val reviewCount: Int,
    val categories: List<ProviderCategoryDto>,
    val services: List<ProviderServiceDto>,
    val avatarUrl: String? = null,
    val galleryImages: List<GalleryImageDto> = emptyList()
)

data class ProviderServiceDto(
    val id: UUID,
    val title: String,
    val description: String?,
    val categoryId: UUID,
    val categoryName: String,
    val pricingType: String,
    val priceAmount: BigDecimal,
    val priceCurrency: String,
    val durationMinutes: Int?
)

data class ProviderTranslationDto(
    val lang: String,
    val businessName: String? = null,
    val description: String? = null
)

data class CreateProviderRequest(
    @field:NotBlank
    val businessName: String,
    val description: String? = null,
    val cityId: UUID? = null,
    val areaId: UUID? = null,
    val addressLine: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val serviceRadiusKm: BigDecimal? = null,
    val isMobile: Boolean = false,
    val categoryIds: List<UUID> = emptyList(),
    val translations: List<ProviderTranslationDto> = emptyList()
)

data class UpdateProviderRequest(
    val businessName: String? = null,
    val description: String? = null,
    val cityId: UUID? = null,
    val areaId: UUID? = null,
    val addressLine: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val serviceRadiusKm: BigDecimal? = null,
    val isMobile: Boolean? = null,
    val categoryIds: List<UUID>? = null,
    val translations: List<ProviderTranslationDto>? = null
)
