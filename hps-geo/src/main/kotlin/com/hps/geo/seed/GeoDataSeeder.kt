package com.hps.geo.seed

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hps.domain.attribute.AttributeDataType
import com.hps.domain.attribute.AttributeDefinition
import com.hps.domain.attribute.AttributeDefinitionTranslation
import com.hps.domain.geo.*
import com.hps.domain.service.ServiceCategory
import com.hps.domain.service.ServiceCategoryTranslation
import com.hps.domain.service.ServiceTemplate
import com.hps.domain.service.ServiceTemplateTranslation
import com.hps.persistence.attribute.AttributeDefinitionRepository
import com.hps.persistence.geo.CountryRepository
import com.hps.persistence.service.ServiceCategoryRepository
import com.hps.persistence.service.ServiceTemplateRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Order(1)
class GeoDataSeeder(
    private val countryRepository: CountryRepository,
    private val categoryRepository: ServiceCategoryRepository,
    private val attributeRepository: AttributeDefinitionRepository,
    private val templateRepository: ServiceTemplateRepository
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(GeoDataSeeder::class.java)
    private val mapper = jacksonObjectMapper()

    @Transactional
    override fun run(args: ApplicationArguments) {
        seedCountries()
        seedCategories()
        seedAttributes()
        seedServiceTemplates()
    }

    private fun seedCountries() {
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

    private fun seedCategories() {
        if (categoryRepository.count() > 0) {
            log.info("Service categories already seeded, skipping")
            return
        }

        log.info("Seeding service categories...")

        val resolver = PathMatchingResourcePatternResolver()
        val resource = resolver.getResource("classpath:seed-data/categories.json")
        val seeds = mapper.readValue<List<CategorySeed>>(resource.inputStream)

        var count = 0
        for (seed in seeds) {
            val category = createCategory(seed, null)
            categoryRepository.save(category)
            count += 1 + category.children.size
        }

        log.info("Seeded $count service categories")
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

    private fun createCategory(seed: CategorySeed, parent: ServiceCategory?): ServiceCategory {
        val category = ServiceCategory(
            parent = parent,
            icon = seed.icon,
            slug = seed.slug,
            imageUrl = seed.imageUrl,
            sortOrder = seed.sortOrder
        )

        seed.translations.forEach { (lang, name) ->
            category.translations.add(
                ServiceCategoryTranslation(category = category, lang = lang, name = name)
            )
        }

        seed.children.forEach { childSeed ->
            val child = createCategory(childSeed, category)
            category.children.add(child)
        }

        return category
    }

    private fun seedAttributes() {
        if (attributeRepository.count() > 0) {
            log.info("Attribute definitions already seeded, skipping")
            return
        }

        log.info("Seeding attribute definitions...")

        val resolver = PathMatchingResourcePatternResolver()
        val resource = resolver.getResource("classpath:seed-data/attributes.json")
        val seeds = mapper.readValue<List<AttributeDefinitionSeed>>(resource.inputStream)

        for (seed in seeds) {
            val def = AttributeDefinition(
                domain = seed.domain,
                key = seed.key,
                dataType = AttributeDataType.valueOf(seed.dataType),
                isRequired = seed.isRequired,
                options = seed.options?.let { mapper.writeValueAsString(it) },
                validation = seed.validation?.let { mapper.writeValueAsString(it) },
                sortOrder = seed.sortOrder
            )

            seed.translations.forEach { (lang, t) ->
                def.translations.add(
                    AttributeDefinitionTranslation(
                        attribute = def,
                        lang = lang,
                        label = t.label,
                        hint = t.hint,
                        optionLabels = t.optionLabels?.let { mapper.writeValueAsString(it) }
                    )
                )
            }

            attributeRepository.save(def)
        }

        log.info("Seeded ${seeds.size} attribute definitions")
    }

    private fun seedServiceTemplates() {
        if (templateRepository.count() > 0) {
            log.info("Service templates already seeded, skipping")
            return
        }

        log.info("Seeding service templates...")

        val resolver = PathMatchingResourcePatternResolver()
        val resource = resolver.getResource("classpath:seed-data/service-templates.json")
        val seeds = mapper.readValue<List<ServiceTemplateCategorySeed>>(resource.inputStream)

        var count = 0
        for (catSeed in seeds) {
            val category = categoryRepository.findBySlug(catSeed.categorySlug)
            if (category == null) {
                log.warn("Category '${catSeed.categorySlug}' not found for templates")
                continue
            }

            for (tmplSeed in catSeed.templates) {
                val template = ServiceTemplate(
                    category = category,
                    slug = tmplSeed.slug,
                    defaultDurationMinutes = tmplSeed.defaultDuration,
                    sortOrder = tmplSeed.sortOrder
                )

                tmplSeed.translations.forEach { (lang, t) ->
                    template.translations.add(
                        ServiceTemplateTranslation(
                            template = template,
                            lang = lang,
                            title = t.title,
                            description = t.description
                        )
                    )
                }

                templateRepository.save(template)
                count++
            }
        }

        log.info("Seeded $count service templates")
    }
}
