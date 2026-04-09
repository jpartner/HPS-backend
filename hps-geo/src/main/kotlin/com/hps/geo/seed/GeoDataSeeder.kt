package com.hps.geo.seed

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hps.domain.attribute.AttributeDataType
import com.hps.domain.attribute.AttributeDefinition
import com.hps.domain.attribute.AttributeDefinitionTranslation
import com.hps.domain.geo.*
import com.hps.domain.reference.ReferenceList
import com.hps.domain.reference.ReferenceListItem
import com.hps.domain.reference.ReferenceListItemTranslation
import com.hps.domain.service.ServiceCategory
import com.hps.domain.service.ServiceCategoryTranslation
import com.hps.domain.service.ServiceTemplate
import com.hps.domain.service.ServiceTemplateTranslation
import com.hps.persistence.attribute.AttributeDefinitionRepository
import com.hps.domain.geo.CountryCurrency
import com.hps.persistence.geo.CountryCurrencyRepository
import com.hps.persistence.geo.CountryRepository
import com.hps.persistence.reference.ReferenceListRepository
import com.hps.persistence.service.ServiceCategoryRepository
import com.hps.persistence.service.ServiceTemplateRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Order(1)
class GeoDataSeeder(
    private val countryRepository: CountryRepository,
    private val countryCurrencyRepository: CountryCurrencyRepository,
    private val categoryRepository: ServiceCategoryRepository,
    private val attributeRepository: AttributeDefinitionRepository,
    private val referenceListRepository: ReferenceListRepository,
    private val templateRepository: ServiceTemplateRepository
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(GeoDataSeeder::class.java)
    private val mapper = jacksonObjectMapper()

    companion object {
        val DEFAULT_TENANT_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    }

    @Transactional
    override fun run(args: ApplicationArguments) {
        seedCountries()
        seedCountryCurrencies()
        seedCategories()
        seedReferenceLists()
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

    private fun seedCountryCurrencies() {
        if (countryCurrencyRepository.count() > 0) {
            log.info("Country currencies already seeded, skipping")
            return
        }

        log.info("Seeding country currencies...")

        // Currency mapping: iso_code -> (primaryCurrency, secondaryCurrency?)
        val currencyMap = mapOf(
            // Eurozone
            "AT" to Pair("EUR", null), "BE" to Pair("EUR", null), "CY" to Pair("EUR", null),
            "DE" to Pair("EUR", null), "EE" to Pair("EUR", null), "ES" to Pair("EUR", null),
            "FI" to Pair("EUR", null), "FR" to Pair("EUR", null), "GR" to Pair("EUR", null),
            "HR" to Pair("EUR", null), "IE" to Pair("EUR", null), "IT" to Pair("EUR", null),
            "LT" to Pair("EUR", null), "LU" to Pair("EUR", null), "LV" to Pair("EUR", null),
            "MT" to Pair("EUR", null), "NL" to Pair("EUR", null), "PT" to Pair("EUR", null),
            "SI" to Pair("EUR", null), "SK" to Pair("EUR", null),
            // Non-eurozone with EUR secondary
            "PL" to Pair("PLN", "EUR"), "CZ" to Pair("CZK", "EUR"), "GB" to Pair("GBP", "EUR"),
            "SE" to Pair("SEK", "EUR"), "DK" to Pair("DKK", "EUR"), "NO" to Pair("NOK", "EUR"),
            "HU" to Pair("HUF", "EUR"), "RO" to Pair("RON", "EUR"), "BG" to Pair("BGN", "EUR"),
            "CH" to Pair("CHF", "EUR"), "UA" to Pair("UAH", "EUR")
        )

        var count = 0
        for ((isoCode, currencies) in currencyMap) {
            val country = countryRepository.findByIsoCode(isoCode) ?: continue
            countryCurrencyRepository.save(
                CountryCurrency(
                    country = country,
                    primaryCurrency = currencies.first,
                    secondaryCurrency = currencies.second
                )
            )
            count++
        }

        log.info("Seeded $count country currencies")
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
            tenantId = DEFAULT_TENANT_ID,
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

    private fun seedReferenceLists() {
        if (referenceListRepository.count() > 0) {
            log.info("Reference lists already seeded, skipping")
            return
        }

        log.info("Seeding reference lists...")

        val resolver = PathMatchingResourcePatternResolver()
        val resource = resolver.getResource("classpath:seed-data/reference-lists.json")
        val seeds = mapper.readValue<List<ReferenceListSeed>>(resource.inputStream)

        for (seed in seeds) {
            val rl = ReferenceList(key = seed.key, name = seed.name)
            seed.items.forEachIndexed { idx, itemSeed ->
                val item = ReferenceListItem(
                    referenceList = rl,
                    value = itemSeed.value,
                    sortOrder = idx
                )
                itemSeed.translations.forEach { (lang, label) ->
                    item.translations.add(
                        ReferenceListItemTranslation(item = item, lang = lang, label = label)
                    )
                }
                rl.items.add(item)
            }
            referenceListRepository.save(rl)
        }

        log.info("Seeded ${seeds.size} reference lists")
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
            val refList = seed.referenceListKey?.let { key ->
                referenceListRepository.findByKey(key)
                    ?: run { log.warn("Reference list '{}' not found for attribute '{}'", key, seed.key); null }
            }

            val def = AttributeDefinition(
                tenantId = DEFAULT_TENANT_ID,
                domain = seed.domain,
                key = seed.key,
                dataType = AttributeDataType.valueOf(seed.dataType),
                isRequired = seed.isRequired,
                options = if (refList != null) null else seed.options?.let { mapper.writeValueAsString(it) },
                validation = seed.validation?.let { mapper.writeValueAsString(it) },
                sortOrder = seed.sortOrder,
                referenceList = refList
            )

            seed.translations.forEach { (lang, t) ->
                def.translations.add(
                    AttributeDefinitionTranslation(
                        attribute = def,
                        lang = lang,
                        label = t.label,
                        hint = t.hint,
                        optionLabels = if (refList != null) null else t.optionLabels?.let { mapper.writeValueAsString(it) }
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
                    tenantId = DEFAULT_TENANT_ID,
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
