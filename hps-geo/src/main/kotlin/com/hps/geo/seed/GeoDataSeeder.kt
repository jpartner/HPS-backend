package com.hps.geo.seed

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hps.domain.geo.*
import com.hps.persistence.geo.CountryRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GeoDataSeeder(
    private val countryRepository: CountryRepository
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(GeoDataSeeder::class.java)
    private val mapper = jacksonObjectMapper()

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (countryRepository.count() > 0) {
            log.info("Geographic data already seeded, skipping")
            return
        }

        log.info("Seeding geographic data...")

        val resolver = PathMatchingResourcePatternResolver()
        val resources = resolver.getResources("classpath:seed-data/countries/*.json")

        var countryCount = 0
        var regionCount = 0
        var cityCount = 0

        for (resource in resources.sortedBy { it.filename }) {
            val seed = mapper.readValue<CountrySeed>(resource.inputStream)
            val country = createCountry(seed)
            countryRepository.save(country)
            countryCount++
            regionCount += country.regions.size
            cityCount += country.regions.sumOf { it.cities.size }
        }

        log.info("Seeded $countryCount countries, $regionCount regions, $cityCount cities")
    }

    private fun createCountry(seed: CountrySeed): Country {
        val country = Country(
            isoCode = seed.isoCode,
            phonePrefix = seed.phonePrefix
        )

        seed.translations.forEach { (lang, name) ->
            country.translations.add(CountryTranslation(country = country, lang = lang, name = name))
        }

        seed.regions.forEach { regionSeed ->
            val region = Region(
                country = country,
                code = regionSeed.code,
                latitude = regionSeed.latitude,
                longitude = regionSeed.longitude
            )

            regionSeed.translations.forEach { (lang, name) ->
                region.translations.add(RegionTranslation(region = region, lang = lang, name = name))
            }

            regionSeed.cities.forEach { citySeed ->
                val city = City(
                    region = region,
                    latitude = citySeed.latitude,
                    longitude = citySeed.longitude,
                    population = citySeed.population
                )

                citySeed.translations.forEach { (lang, name) ->
                    city.translations.add(CityTranslation(city = city, lang = lang, name = name))
                }

                region.cities.add(city)
            }

            country.regions.add(region)
        }

        return country
    }
}
