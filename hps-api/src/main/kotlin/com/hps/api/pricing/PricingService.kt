package com.hps.api.pricing

import com.hps.common.exception.BadRequestException
import com.hps.common.exception.NotFoundException
import com.hps.common.i18n.bestTranslation
import com.hps.domain.service.RateType
import com.hps.persistence.geo.CountryCurrencyRepository
import com.hps.persistence.service.ProviderRateRepository
import com.hps.persistence.service.ServiceRepository
import com.hps.persistence.user.ProviderProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class PricingService(
    private val providerRateRepository: ProviderRateRepository,
    private val serviceRepository: ServiceRepository,
    private val countryCurrencyRepository: CountryCurrencyRepository,
    private val providerProfileRepository: ProviderProfileRepository
) {

    fun calculate(request: PricingCalculateRequest, lang: String): PricingBreakdownDto {
        val provider = providerProfileRepository.findById(request.providerId)
            .orElseThrow { NotFoundException("Provider", request.providerId) }

        // Resolve currencies: provider → city → region → country → countryCurrency
        val country = provider.city?.region?.country
            ?: throw BadRequestException("Provider has no location set; cannot resolve currency")

        val cc = countryCurrencyRepository.findByCountryId(country.id)
        val primaryCurrency = cc?.primaryCurrency ?: "EUR"
        val secondaryCurrency = cc?.secondaryCurrency

        // Find matching rate
        val rateType = RateType.valueOf(request.rateType)
        val rates = providerRateRepository.findByProviderIdAndIsActiveTrue(request.providerId)
        val matchedRate = rates.find { rate ->
            rate.rateType == rateType && rate.durationMinutes == request.durationMinutes
        }

        val baseRate = matchedRate?.let {
            PricingLineDto(
                rateType = it.rateType.name,
                durationMinutes = it.durationMinutes,
                primaryAmount = it.primaryAmount,
                secondaryAmount = it.secondaryAmount
            )
        }

        // Load extras
        val extras = if (request.extraIds.isNotEmpty()) {
            val services = serviceRepository.findActiveByIdsWithTranslations(request.extraIds, lang)

            // Validate all belong to provider
            val invalidIds = services.filter { it.provider.userId != request.providerId }.map { it.id }
            if (invalidIds.isNotEmpty()) {
                throw BadRequestException("Extras do not belong to this provider: $invalidIds")
            }

            // Check requested IDs were all found
            val foundIds = services.map { it.id }.toSet()
            val missingIds = request.extraIds.filter { it !in foundIds }
            if (missingIds.isNotEmpty()) {
                throw BadRequestException("Extras not found: $missingIds")
            }

            services.map { svc ->
                PricingExtraLineDto(
                    extraId = svc.id,
                    name = svc.translations.bestTranslation(lang, { it.lang }, { it.title }),
                    isIncluded = svc.isIncluded,
                    primaryAmount = svc.primaryAmount,
                    secondaryAmount = svc.secondaryAmount
                )
            }
        } else {
            emptyList()
        }

        // Calculate totals
        val basePrimary = baseRate?.primaryAmount ?: BigDecimal.ZERO
        val extrasPrimary = extras.filter { !it.isIncluded }.sumOf { it.primaryAmount }
        val totalPrimary = basePrimary + extrasPrimary

        val totalSecondary = if (secondaryCurrency != null) {
            val baseSecondary = baseRate?.secondaryAmount ?: BigDecimal.ZERO
            val extrasSecondary = extras.filter { !it.isIncluded }
                .mapNotNull { it.secondaryAmount }
                .fold(BigDecimal.ZERO) { acc, v -> acc + v }
            baseSecondary + extrasSecondary
        } else {
            null
        }

        return PricingBreakdownDto(
            primaryCurrency = primaryCurrency,
            secondaryCurrency = secondaryCurrency,
            baseRate = baseRate,
            extras = extras,
            totalPrimaryAmount = totalPrimary,
            totalSecondaryAmount = totalSecondary
        )
    }
}
