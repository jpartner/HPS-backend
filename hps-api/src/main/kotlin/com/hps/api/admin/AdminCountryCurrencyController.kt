package com.hps.api.admin

import com.hps.common.exception.NotFoundException
import com.hps.domain.geo.CountryCurrency
import com.hps.persistence.geo.CountryCurrencyRepository
import com.hps.persistence.geo.CountryRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class CountryCurrencyDto(
    val countryId: UUID,
    val isoCode: String,
    val countryName: String,
    val primaryCurrency: String,
    val secondaryCurrency: String?
)

data class UpsertCountryCurrencyRequest(
    val primaryCurrency: String,
    val secondaryCurrency: String? = null
)

@RestController
@RequestMapping("/api/v1/admin/country-currencies")
class AdminCountryCurrencyController(
    private val countryCurrencyRepository: CountryCurrencyRepository,
    private val countryRepository: CountryRepository
) {

    @GetMapping
    @Transactional(readOnly = true)
    fun listAll(): List<CountryCurrencyDto> {
        return countryCurrencyRepository.findAllWithCountry().map { cc ->
            val name = cc.country.translations.firstOrNull { it.lang == "en" }?.name
                ?: cc.country.isoCode
            CountryCurrencyDto(
                countryId = cc.country.id,
                isoCode = cc.country.isoCode,
                countryName = name,
                primaryCurrency = cc.primaryCurrency,
                secondaryCurrency = cc.secondaryCurrency
            )
        }
    }

    @PutMapping("/{countryId}")
    @Transactional
    fun upsert(
        @PathVariable countryId: UUID,
        @RequestBody request: UpsertCountryCurrencyRequest
    ): CountryCurrencyDto {
        val country = countryRepository.findById(countryId)
            .orElseThrow { NotFoundException("Country", countryId) }

        val cc = countryCurrencyRepository.findByCountryId(countryId)
            ?.also {
                it.primaryCurrency = request.primaryCurrency
                it.secondaryCurrency = request.secondaryCurrency
            }
            ?: CountryCurrency(
                country = country,
                primaryCurrency = request.primaryCurrency,
                secondaryCurrency = request.secondaryCurrency
            )

        countryCurrencyRepository.save(cc)

        val name = country.translations.firstOrNull { it.lang == "en" }?.name ?: country.isoCode
        return CountryCurrencyDto(
            countryId = country.id,
            isoCode = country.isoCode,
            countryName = name,
            primaryCurrency = cc.primaryCurrency,
            secondaryCurrency = cc.secondaryCurrency
        )
    }
}
