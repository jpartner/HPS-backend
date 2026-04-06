package com.hps.geo.seed

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hps.domain.scheduling.ProviderScheduleSettings
import com.hps.domain.scheduling.ProviderWeeklySlot
import com.hps.domain.service.PricingType
import com.hps.domain.service.Service
import com.hps.domain.service.ServiceImage
import com.hps.domain.service.ServiceTranslation
import com.hps.domain.user.ProviderProfile
import com.hps.domain.user.User
import com.hps.domain.user.UserProfile
import com.hps.domain.user.UserRole
import com.hps.persistence.geo.CityRepository
import com.hps.persistence.scheduling.ScheduleSettingsRepository
import com.hps.persistence.scheduling.WeeklySlotRepository
import com.hps.persistence.service.ServiceCategoryRepository
import com.hps.persistence.service.ServiceRepository
import com.hps.persistence.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Component
@Order(2)
class ProviderSeeder(
    private val userRepository: UserRepository,
    private val cityRepository: CityRepository,
    private val categoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
    private val scheduleSettingsRepository: ScheduleSettingsRepository,
    private val weeklySlotRepository: WeeklySlotRepository,
    private val providerRepository: com.hps.persistence.user.ProviderProfileRepository
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(ProviderSeeder::class.java)
    private val mapper = jacksonObjectMapper()
    private val passwordEncoder = BCryptPasswordEncoder()

    companion object {
        val DEFAULT_TENANT_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    }

    private val timezoneByCountry = mapOf(
        "PL" to "Europe/Warsaw",
        "DE" to "Europe/Berlin",
        "UA" to "Europe/Kyiv",
        "CZ" to "Europe/Prague",
        "HU" to "Europe/Budapest",
        "HR" to "Europe/Zagreb",
        "SK" to "Europe/Bratislava",
        "EE" to "Europe/Tallinn",
        "LV" to "Europe/Riga",
        "LT" to "Europe/Vilnius"
    )

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping provider seeding")
            return
        }

        log.info("Seeding example providers...")

        val resolver = PathMatchingResourcePatternResolver()
        val resource = resolver.getResource("classpath:seed-data/providers.json")
        val seeds = mapper.readValue<List<ProviderSeed>>(resource.inputStream)

        // Pre-load all categories with translations
        val allCategories = categoryRepository.findAllWithTranslations()

        // Build lookup maps: English name -> category
        val parentCategoryByName = mutableMapOf<String, com.hps.domain.service.ServiceCategory>()
        val subcategoryByPath = mutableMapOf<String, com.hps.domain.service.ServiceCategory>()

        for (category in allCategories) {
            val parentEnName = category.translations.find { it.lang == "en" }?.name ?: continue
            if (category.parent == null) {
                parentCategoryByName[parentEnName] = category
                for (child in category.children) {
                    val childEnName = child.translations.find { it.lang == "en" }?.name ?: continue
                    subcategoryByPath["$parentEnName > $childEnName"] = child
                }
            }
        }

        val hashedPassword = passwordEncoder.encode("demo123")
        var providerCount = 0
        var serviceCount = 0

        for (seed in seeds) {
            val city = cityRepository.findByEnglishNameAndCountryCode(seed.cityName, seed.countryCode)
            if (city == null) {
                log.warn("City '${seed.cityName}' not found for country '${seed.countryCode}', skipping provider '${seed.email}'")
                continue
            }

            // Create User
            val user = User(
                email = seed.email,
                tenantId = DEFAULT_TENANT_ID,
                passwordHash = hashedPassword,
                role = UserRole.PROVIDER,
                avatarUrl = seed.avatarUrl
            )

            // Create UserProfile
            val profile = UserProfile(
                user = user,
                firstName = seed.firstName,
                lastName = seed.lastName
            )
            user.profile = profile

            // Create ProviderProfile
            val providerProfile = ProviderProfile(
                user = user,
                tenantId = DEFAULT_TENANT_ID,
                businessName = seed.businessName,
                description = seed.description,
                city = city,
                latitude = seed.latitude,
                longitude = seed.longitude,
                isMobile = seed.isMobile,
                serviceRadiusKm = seed.serviceRadiusKm?.let { BigDecimal.valueOf(it) },
                isVerified = true
            )

            // Link categories
            for (categoryName in seed.categories) {
                val category = parentCategoryByName[categoryName]
                if (category != null) {
                    providerProfile.categories.add(category)
                } else {
                    log.warn("Category '$categoryName' not found for provider '${seed.email}'")
                }
            }

            user.providerProfile = providerProfile

            // Save user (cascades to profile and providerProfile)
            val savedUser = userRepository.saveAndFlush(user)
            val savedProvider = savedUser.providerProfile!!

            // Add profile translations
            for ((lang, translationSeed) in seed.profileTranslations) {
                savedProvider.translations.add(
                    com.hps.domain.user.ProviderProfileTranslation(
                        provider = savedProvider,
                        lang = lang,
                        businessName = translationSeed.businessName,
                        description = translationSeed.description
                    )
                )
            }
            if (seed.profileTranslations.isNotEmpty()) {
                providerRepository.save(savedProvider)
            }

            // Create services
            for (serviceSeed in seed.services) {
                val subcategory = subcategoryByPath[serviceSeed.categoryPath]
                if (subcategory == null) {
                    log.warn("Subcategory '${serviceSeed.categoryPath}' not found, skipping service")
                    continue
                }

                val service = serviceRepository.save(Service(
                    tenantId = DEFAULT_TENANT_ID,
                    provider = savedProvider,
                    category = subcategory,
                    pricingType = PricingType.valueOf(serviceSeed.pricingType),
                    priceAmount = BigDecimal.valueOf(serviceSeed.priceAmount),
                    priceCurrency = serviceSeed.priceCurrency,
                    durationMinutes = serviceSeed.durationMinutes
                ))

                for ((lang, translationSeed) in serviceSeed.translations) {
                    service.translations.add(
                        ServiceTranslation(
                            service = service,
                            lang = lang,
                            title = translationSeed.title,
                            description = translationSeed.description
                        )
                    )
                }

                if (serviceSeed.imageUrl != null) {
                    service.images.add(
                        ServiceImage(
                            service = service,
                            url = serviceSeed.imageUrl,
                            sortOrder = 0
                        )
                    )
                }

                serviceRepository.save(service)
                savedProvider.services.add(service)
                serviceCount++
            }

            // Create gallery images (seeded as APPROVED)
            seed.galleryImages.forEachIndexed { index, imageSeed ->
                savedProvider.media.add(
                    com.hps.domain.user.ProviderMedia(
                        provider = savedProvider,
                        url = imageSeed.url,
                        caption = imageSeed.caption,
                        sortOrder = index,
                        approvalStatus = com.hps.domain.user.MediaApprovalStatus.APPROVED
                    )
                )
            }
            if (seed.galleryImages.isNotEmpty()) {
                providerRepository.save(savedProvider)
            }

            // Create schedule settings
            val timezone = timezoneByCountry[seed.countryCode] ?: "Europe/Berlin"
            val scheduleSettings = ProviderScheduleSettings(
                provider = savedProvider,
                timezone = timezone
            )
            scheduleSettingsRepository.save(scheduleSettings)

            // Create default weekly slots (Mon-Fri 09:00-17:00, Sat 10:00-14:00)
            val weekdayStart = LocalTime.of(9, 0)
            val weekdayEnd = LocalTime.of(17, 0)
            val saturdayStart = LocalTime.of(10, 0)
            val saturdayEnd = LocalTime.of(14, 0)

            for (day in DayOfWeek.MONDAY.value..DayOfWeek.FRIDAY.value) {
                weeklySlotRepository.save(
                    ProviderWeeklySlot(
                        provider = savedProvider,
                        dayOfWeek = day,
                        startTime = weekdayStart,
                        endTime = weekdayEnd
                    )
                )
            }
            weeklySlotRepository.save(
                ProviderWeeklySlot(
                    provider = savedProvider,
                    dayOfWeek = DayOfWeek.SATURDAY.value,
                    startTime = saturdayStart,
                    endTime = saturdayEnd
                )
            )

            providerCount++
        }

        log.info("Seeded $providerCount example providers with $serviceCount services")
    }
}
