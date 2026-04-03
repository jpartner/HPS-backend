package com.hps.api.provider

import com.hps.common.dto.PageMeta
import com.hps.common.dto.PageResponse
import com.hps.common.exception.BadRequestException
import com.hps.common.exception.NotFoundException
import com.hps.common.i18n.bestTranslation
import com.hps.domain.user.ProviderProfile
import com.hps.domain.user.ProviderProfileTranslation
import com.hps.domain.user.UserRole
import com.hps.persistence.geo.AreaRepository
import com.hps.persistence.geo.CityRepository
import com.hps.persistence.service.ServiceCategoryRepository
import com.hps.persistence.service.ServiceRepository
import com.hps.persistence.user.ProviderProfileRepository
import com.hps.persistence.user.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ProviderService(
    private val providerRepository: ProviderProfileRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: ServiceCategoryRepository,
    private val cityRepository: CityRepository,
    private val areaRepository: AreaRepository,
    private val serviceRepository: ServiceRepository
) {
    fun listByCategory(
        categoryId: UUID,
        countryCode: String?,
        regionId: UUID?,
        cityId: UUID?,
        page: Int,
        size: Int,
        lang: String
    ): PageResponse<ProviderSummaryDto> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "avgRating"))

        val result = when {
            cityId != null -> providerRepository.findByCategoryIdAndCityId(categoryId, cityId, pageable)
            regionId != null -> providerRepository.findByCategoryIdAndRegionId(categoryId, regionId, pageable)
            countryCode != null -> providerRepository.findByCategoryIdAndCountryCode(categoryId, countryCode.uppercase(), pageable)
            else -> providerRepository.findByCategoryId(categoryId, pageable)
        }

        return PageResponse(
            data = result.content.map { it.toSummaryDto(lang) },
            meta = PageMeta(page = page, pageSize = size, totalItems = result.totalElements)
        )
    }

    fun getDetail(providerId: UUID, lang: String): ProviderDetailDto {
        val provider = providerRepository.findById(providerId)
            .orElseThrow { NotFoundException("Provider", providerId) }

        val services = serviceRepository.findByProviderWithTranslations(providerId, lang)

        return ProviderDetailDto(
            id = provider.userId!!,
            businessName = provider.translatedBusinessName(lang),
            description = provider.translatedDescription(lang),
            email = provider.user.email,
            phone = provider.user.phone,
            cityName = provider.city?.translations?.bestTranslation(lang, { it.lang }, { it.name }),
            areaName = provider.area?.translations?.bestTranslation(lang, { it.lang }, { it.name }),
            addressLine = provider.addressLine,
            latitude = provider.latitude,
            longitude = provider.longitude,
            serviceRadiusKm = provider.serviceRadiusKm,
            isMobile = provider.isMobile,
            isVerified = provider.isVerified,
            avgRating = provider.avgRating,
            reviewCount = provider.reviewCount,
            categories = provider.categories.map {
                ProviderCategoryDto(it.id, it.translations.bestTranslation(lang, { t -> t.lang }, { t -> t.name }))
            },
            services = services.map { it.toServiceDto(lang) },
            avatarUrl = provider.user.avatarUrl
        )
    }

    @Transactional
    fun createProfile(userId: UUID, request: CreateProviderRequest): ProviderDetailDto {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User", userId) }

        if (user.providerProfile != null) {
            throw BadRequestException("Provider profile already exists")
        }

        val provider = ProviderProfile(user = user)
        provider.businessName = request.businessName
        provider.description = request.description
        provider.addressLine = request.addressLine
        provider.latitude = request.latitude
        provider.longitude = request.longitude
        provider.serviceRadiusKm = request.serviceRadiusKm
        provider.isMobile = request.isMobile

        if (request.cityId != null) {
            provider.city = cityRepository.findById(request.cityId).orElse(null)
        }
        if (request.areaId != null) {
            provider.area = areaRepository.findById(request.areaId).orElse(null)
        }

        if (request.categoryIds.isNotEmpty()) {
            val categories = categoryRepository.findAllById(request.categoryIds)
            provider.categories.addAll(categories)
        }

        request.translations.forEach { t ->
            provider.translations.add(
                ProviderProfileTranslation(
                    provider = provider, lang = t.lang,
                    businessName = t.businessName, description = t.description
                )
            )
        }

        user.role = UserRole.PROVIDER
        user.providerProfile = provider
        userRepository.save(user)

        return getDetail(userId, "en")
    }

    @Transactional
    fun updateProfile(userId: UUID, request: UpdateProviderRequest): ProviderDetailDto {
        val provider = providerRepository.findById(userId)
            .orElseThrow { NotFoundException("Provider", userId) }

        request.businessName?.let { provider.businessName = it }
        request.description?.let { provider.description = it }
        request.addressLine?.let { provider.addressLine = it }
        request.latitude?.let { provider.latitude = it }
        request.longitude?.let { provider.longitude = it }
        request.serviceRadiusKm?.let { provider.serviceRadiusKm = it }
        request.isMobile?.let { provider.isMobile = it }

        request.cityId?.let { provider.city = cityRepository.findById(it).orElse(null) }
        request.areaId?.let { provider.area = areaRepository.findById(it).orElse(null) }

        if (request.categoryIds != null) {
            provider.categories.clear()
            if (request.categoryIds.isNotEmpty()) {
                val categories = categoryRepository.findAllById(request.categoryIds)
                provider.categories.addAll(categories)
            }
        }

        if (request.translations != null) {
            provider.translations.clear()
            providerRepository.saveAndFlush(provider)
            request.translations.forEach { t ->
                provider.translations.add(
                    ProviderProfileTranslation(
                        provider = provider, lang = t.lang,
                        businessName = t.businessName, description = t.description
                    )
                )
            }
        }

        provider.updatedAt = Instant.now()
        providerRepository.save(provider)

        return getDetail(userId, "en")
    }

    private fun ProviderProfile.translatedBusinessName(lang: String): String? {
        val t = translations.firstOrNull { it.lang == lang }
        return t?.businessName ?: translations.firstOrNull { it.lang == "en" }?.businessName ?: businessName
    }

    private fun ProviderProfile.translatedDescription(lang: String): String? {
        val t = translations.firstOrNull { it.lang == lang }
        return t?.description ?: translations.firstOrNull { it.lang == "en" }?.description ?: description
    }

    private fun ProviderProfile.toSummaryDto(lang: String) = ProviderSummaryDto(
        id = userId!!,
        businessName = translatedBusinessName(lang),
        description = translatedDescription(lang),
        cityName = city?.translations?.bestTranslation(lang, { it.lang }, { it.name }),
        areaName = area?.translations?.bestTranslation(lang, { it.lang }, { it.name }),
        latitude = latitude,
        longitude = longitude,
        isMobile = isMobile,
        isVerified = isVerified,
        avgRating = avgRating,
        reviewCount = reviewCount,
        categories = categories.map {
            ProviderCategoryDto(it.id, it.translations.bestTranslation(lang, { t -> t.lang }, { t -> t.name }))
        },
        avatarUrl = user.avatarUrl
    )

    private fun com.hps.domain.service.Service.toServiceDto(lang: String) = ProviderServiceDto(
        id = id,
        title = translations.bestTranslation(lang, { it.lang }, { it.title }),
        description = translations.firstOrNull { it.lang == lang }?.description
            ?: translations.firstOrNull { it.lang == "en" }?.description,
        categoryId = category.id,
        categoryName = category.translations.bestTranslation(lang, { it.lang }, { it.name }),
        pricingType = pricingType.name,
        priceAmount = priceAmount,
        priceCurrency = priceCurrency,
        durationMinutes = durationMinutes
    )
}
