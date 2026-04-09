package com.hps.api.provider

import com.hps.api.auth.userId
import com.hps.common.exception.BadRequestException
import com.hps.common.exception.NotFoundException
import com.hps.common.i18n.bestTranslation
import com.hps.common.tenant.TenantContext
import com.hps.domain.service.ProviderRate
import com.hps.persistence.geo.CountryCurrencyRepository
import com.hps.persistence.service.ProviderRateRepository
import com.hps.persistence.service.ServiceCategoryRepository
import com.hps.persistence.user.ProviderProfileRepository
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/v1/providers/me/rates")
class ProviderRateController(
    private val providerRateRepository: ProviderRateRepository,
    private val providerProfileRepository: ProviderProfileRepository,
    private val countryCurrencyRepository: CountryCurrencyRepository,
    private val categoryRepository: ServiceCategoryRepository
) {

    @GetMapping
    @Transactional(readOnly = true)
    fun getRateCard(auth: Authentication): ProviderRateCardDto {
        val providerId = auth.userId()
        val provider = providerProfileRepository.findById(providerId)
            .orElseThrow { NotFoundException("Provider", providerId) }

        val (primaryCurrency, secondaryCurrency) = resolveCurrencies(provider)
        val rates = providerRateRepository.findByProviderIdAndIsActiveTrue(providerId)

        return buildRateCardDto(primaryCurrency, secondaryCurrency, rates, "en")
    }

    @PutMapping
    @Transactional
    fun updateRateCard(
        @RequestBody request: UpdateRateCardRequest,
        auth: Authentication
    ): ProviderRateCardDto {
        val providerId = auth.userId()
        val provider = providerProfileRepository.findById(providerId)
            .orElseThrow { NotFoundException("Provider", providerId) }
        val tenantId = TenantContext.require()

        // Clear existing rates
        providerRateRepository.deleteByProviderId(providerId)
        providerRateRepository.flush()

        val now = Instant.now()
        val newRates = mutableListOf<ProviderRate>()

        for (mtInput in request.meetingTypes) {
            val meetingType = categoryRepository.findById(mtInput.meetingTypeId)
                .orElseThrow { NotFoundException("Category", mtInput.meetingTypeId) }

            for ((idx, input) in mtInput.rates.withIndex()) {
                if (input.incallAmount == null && input.outcallAmount == null) {
                    continue // skip rows with no prices
                }
                newRates += ProviderRate(
                    provider = provider,
                    tenantId = tenantId,
                    meetingType = meetingType,
                    durationMinutes = input.durationMinutes,
                    label = input.label,
                    incallAmount = input.incallAmount,
                    outcallAmount = input.outcallAmount,
                    secondaryIncallAmount = input.secondaryIncallAmount,
                    secondaryOutcallAmount = input.secondaryOutcallAmount,
                    sortOrder = input.sortOrder.takeIf { it > 0 } ?: idx,
                    createdAt = now,
                    updatedAt = now
                )
            }
        }

        providerRateRepository.saveAll(newRates)

        val (primaryCurrency, secondaryCurrency) = resolveCurrencies(provider)
        return buildRateCardDto(primaryCurrency, secondaryCurrency, newRates, "en")
    }

    private fun resolveCurrencies(provider: com.hps.domain.user.ProviderProfile): Pair<String, String?> {
        val country = provider.city?.region?.country
            ?: throw BadRequestException("Provider has no location set; cannot resolve currency")

        val cc = countryCurrencyRepository.findByCountryId(country.id)
            ?: return Pair("EUR", null)

        return Pair(cc.primaryCurrency, cc.secondaryCurrency)
    }

    private fun buildRateCardDto(
        primaryCurrency: String,
        secondaryCurrency: String?,
        rates: List<ProviderRate>,
        lang: String
    ): ProviderRateCardDto {
        val byMeetingType = rates.groupBy { it.meetingType?.id }

        val meetingTypes = byMeetingType.map { (mtId, mtRates) ->
            val mt = mtRates.first().meetingType
            MeetingTypeRatesDto(
                meetingTypeId = mtId ?: throw BadRequestException("Rate missing meeting type"),
                meetingTypeName = mt?.translations?.bestTranslation(lang, { it.lang }, { it.name }) ?: "Unknown",
                rates = mtRates.sortedBy { it.sortOrder }.map {
                    RateRowDto(
                        durationMinutes = it.durationMinutes,
                        label = it.label,
                        incallAmount = it.incallAmount,
                        outcallAmount = it.outcallAmount,
                        secondaryIncallAmount = it.secondaryIncallAmount,
                        secondaryOutcallAmount = it.secondaryOutcallAmount,
                        sortOrder = it.sortOrder
                    )
                }
            )
        }

        return ProviderRateCardDto(
            primaryCurrency = primaryCurrency,
            secondaryCurrency = secondaryCurrency,
            meetingTypes = meetingTypes
        )
    }
}
