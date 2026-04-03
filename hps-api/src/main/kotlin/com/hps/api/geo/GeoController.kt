package com.hps.api.geo

import com.hps.common.i18n.LanguageContext
import com.hps.common.i18n.bestTranslation
import com.hps.persistence.geo.AreaRepository
import com.hps.persistence.geo.CityRepository
import com.hps.persistence.geo.CountryRepository
import com.hps.persistence.geo.RegionRepository
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class GeoController(
    private val countryRepository: CountryRepository,
    private val regionRepository: RegionRepository,
    private val cityRepository: CityRepository,
    private val areaRepository: AreaRepository
) {
    @GetMapping("/countries")
    fun listCountries(): List<CountryDto> {
        val lang = LanguageContext.get().code
        return countryRepository.findAllWithTranslations(lang).map { country ->
            CountryDto(
                id = country.id,
                isoCode = country.isoCode,
                phonePrefix = country.phonePrefix,
                name = country.translations.bestTranslation(lang, { it.lang }, { it.name })
            )
        }
    }

    @GetMapping("/countries/{code}/regions")
    fun listRegions(@PathVariable code: String): List<RegionDto> {
        val lang = LanguageContext.get().code
        return regionRepository.findByCountryCodeWithTranslations(code.uppercase(), lang).map { region ->
            RegionDto(
                id = region.id,
                code = region.code,
                name = region.translations.bestTranslation(lang, { it.lang }, { it.name }),
                latitude = region.latitude,
                longitude = region.longitude
            )
        }
    }

    @GetMapping("/regions/{id}/cities")
    fun listCities(@PathVariable id: UUID): List<CityDto> {
        val lang = LanguageContext.get().code
        return cityRepository.findByRegionIdWithTranslations(id, lang).map { city ->
            CityDto(
                id = city.id,
                name = city.translations.bestTranslation(lang, { it.lang }, { it.name }),
                latitude = city.latitude,
                longitude = city.longitude,
                population = city.population
            )
        }
    }

    @GetMapping("/cities/{id}/areas")
    fun listAreas(@PathVariable id: UUID): List<AreaDto> {
        val lang = LanguageContext.get().code
        return areaRepository.findByCityIdWithTranslations(id, lang).map { area ->
            AreaDto(
                id = area.id,
                name = area.translations.bestTranslation(lang, { it.lang }, { it.name }),
                latitude = area.latitude,
                longitude = area.longitude
            )
        }
    }
}
