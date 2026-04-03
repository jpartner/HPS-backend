package com.hps.geo.seed

data class CountrySeed(
    val isoCode: String,
    val phonePrefix: String,
    val translations: Map<String, String>,
    val regions: List<RegionSeed>
)

data class RegionSeed(
    val code: String,
    val latitude: Double,
    val longitude: Double,
    val translations: Map<String, String>,
    val cities: List<CitySeed>
)

data class CitySeed(
    val latitude: Double,
    val longitude: Double,
    val population: Int,
    val translations: Map<String, String>
)

data class CategorySeed(
    val icon: String?,
    val sortOrder: Int,
    val slug: String? = null,
    val imageUrl: String? = null,
    val translations: Map<String, String>,
    val children: List<CategorySeed> = emptyList()
)

data class ProviderSeed(
    val email: String,
    val firstName: String,
    val lastName: String,
    val businessName: String,
    val description: String,
    val avatarUrl: String?,
    val countryCode: String,
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val isMobile: Boolean,
    val serviceRadiusKm: Double?,
    val categories: List<String>,
    val services: List<ProviderServiceSeed>,
    val profileTranslations: Map<String, ProfileTranslationSeed> = emptyMap(),
    val galleryImages: List<GalleryImageSeed> = emptyList()
)

data class GalleryImageSeed(
    val url: String,
    val caption: String? = null
)

data class ProfileTranslationSeed(
    val businessName: String? = null,
    val description: String? = null
)

data class ProviderServiceSeed(
    val categoryPath: String,
    val pricingType: String,
    val priceAmount: Double,
    val priceCurrency: String,
    val durationMinutes: Int,
    val imageUrl: String?,
    val translations: Map<String, ServiceTranslationSeed>
)

data class ServiceTranslationSeed(
    val title: String,
    val description: String?
)

data class AttributeDefinitionSeed(
    val domain: String,
    val key: String,
    val dataType: String = "TEXT",
    val isRequired: Boolean = false,
    val options: List<String>? = null,
    val validation: Map<String, Any>? = null,
    val sortOrder: Int = 0,
    val translations: Map<String, AttributeTranslationSeed>
)

data class AttributeTranslationSeed(
    val label: String,
    val hint: String? = null,
    val optionLabels: Map<String, String>? = null
)

data class ServiceTemplateCategorySeed(
    val categorySlug: String,
    val templates: List<ServiceTemplateSeed>
)

data class ServiceTemplateSeed(
    val slug: String,
    val defaultDuration: Int? = null,
    val sortOrder: Int = 0,
    val translations: Map<String, ServiceTranslationSeed>
)
