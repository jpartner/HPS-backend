package com.hps.api.admin

import com.hps.common.exception.NotFoundException
import com.hps.domain.attribute.AttributeDefinition
import com.hps.domain.user.UserRole
import com.hps.domain.service.ServiceCategory
import com.hps.domain.service.ServiceTemplate
import com.hps.persistence.attribute.AttributeDefinitionRepository
import com.hps.persistence.booking.BookingRepository
import com.hps.persistence.messaging.ConversationRepository
import com.hps.persistence.messaging.MessageRepository
import com.hps.persistence.review.ReviewRepository
import com.hps.persistence.service.ServiceCategoryRepository
import com.hps.persistence.service.ServiceTemplateRepository
import com.hps.persistence.tenant.TenantRepository
import com.hps.persistence.user.ProviderProfileRepository
import com.hps.persistence.user.UserRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// ---------------------------------------------------------------------------
// Config DTOs (used for both export and import)
// ---------------------------------------------------------------------------

data class TenantConfigExport(
    val version: Int = 1,
    val tenant: TenantConfigData,
    val categories: List<CategoryConfigData> = emptyList(),
    val serviceTemplates: List<ServiceTemplateConfigData> = emptyList(),
    val attributes: List<AttributeConfigData> = emptyList(),
    val admin: AdminCredentials? = null,
    val data: TenantRuntimeData? = null
)

data class TenantConfigData(
    val name: String,
    val slug: String,
    val domain: String? = null,
    val defaultLang: String,
    val supportedLangs: String,
    val defaultCurrency: String,
    val settings: String = "{}"
)

data class CategoryConfigData(
    val slug: String?,
    val icon: String?,
    val imageUrl: String?,
    val sortOrder: Int,
    val translations: List<CategoryTranslationEntry>,
    val children: List<CategoryConfigData> = emptyList()
)

data class ServiceTemplateConfigData(
    val slug: String,
    val categorySlug: String,
    val defaultDurationMinutes: Int?,
    val sortOrder: Int,
    val translations: List<TemplateTranslationEntry>
)

data class AttributeConfigData(
    val domain: String,
    val key: String,
    val dataType: String,
    val isRequired: Boolean,
    val options: List<String>?,
    val validation: AdminValidationRules?,
    val sortOrder: Int,
    val translations: List<AttributeTranslationEntry>
)

data class AdminCredentials(
    val email: String,
    val password: String,
    val firstName: String = "Admin",
    val lastName: String = "User"
)

// ---------------------------------------------------------------------------
// Runtime data DTOs (export-only)
// ---------------------------------------------------------------------------

data class TenantRuntimeData(
    val users: List<UserExportData>,
    val providers: List<ProviderExportData>,
    val bookings: List<BookingExportData>,
    val reviews: List<ReviewExportData>,
    val conversations: List<ConversationExportData>
)

data class UserExportData(
    val id: UUID,
    val email: String,
    val role: String,
    val phone: String?,
    val preferredLang: String,
    val isActive: Boolean,
    val firstName: String?,
    val lastName: String?,
    val bio: String?,
    val createdAt: Instant
)

data class ProviderExportData(
    val userId: UUID,
    val email: String,
    val businessName: String?,
    val description: String?,
    val addressLine: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isMobile: Boolean,
    val isVerified: Boolean,
    val avgRating: BigDecimal,
    val reviewCount: Int,
    val attributes: String,
    val categorySlugs: List<String>,
    val translations: List<ProviderTranslationExportData>,
    val services: List<ServiceExportData>,
    val galleryImages: List<GalleryImageExportData>,
    val createdAt: Instant
)

data class ProviderTranslationExportData(
    val lang: String,
    val businessName: String?,
    val description: String?
)

data class ServiceExportData(
    val categorySlug: String?,
    val templateSlug: String?,
    val pricingType: String,
    val priceAmount: BigDecimal,
    val priceCurrency: String,
    val durationMinutes: Int?,
    val isActive: Boolean,
    val translations: List<ServiceTranslationExportData>,
    val createdAt: Instant
)

data class ServiceTranslationExportData(
    val lang: String,
    val title: String,
    val description: String?
)

data class GalleryImageExportData(
    val url: String,
    val caption: String?,
    val sortOrder: Int
)

data class BookingExportData(
    val id: UUID,
    val clientEmail: String,
    val providerEmail: String,
    val status: String,
    val bookingType: String,
    val scheduledAt: Instant,
    val durationMinutes: Int?,
    val priceAmount: BigDecimal,
    val priceCurrency: String,
    val clientNotes: String?,
    val providerNotes: String?,
    val services: List<BookingServiceExportData>,
    val createdAt: Instant
)

data class BookingServiceExportData(
    val serviceTitle: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
    val durationMinutes: Int?
)

data class ReviewExportData(
    val reviewerEmail: String,
    val revieweeEmail: String,
    val direction: String,
    val rating: Short,
    val comment: String?,
    val isVisible: Boolean,
    val createdAt: Instant
)

data class ConversationExportData(
    val participant1Email: String,
    val participant2Email: String,
    val conversationType: String,
    val topic: String?,
    val messages: List<MessageExportData>,
    val createdAt: Instant
)

data class MessageExportData(
    val senderEmail: String,
    val content: String,
    val createdAt: Instant
)

// ---------------------------------------------------------------------------
// Response DTOs
// ---------------------------------------------------------------------------

data class TenantImportResponse(
    val tenantId: UUID,
    val slug: String,
    val categoriesCreated: Int,
    val templatesCreated: Int,
    val attributesCreated: Int,
    val adminCreated: Boolean
)

// ---------------------------------------------------------------------------
// Controller
// ---------------------------------------------------------------------------

@RestController
@RequestMapping("/api/v1/admin/tenants")
class AdminTenantConfigController(
    private val tenantRepository: TenantRepository,
    private val categoryRepository: ServiceCategoryRepository,
    private val templateRepository: ServiceTemplateRepository,
    private val definitionRepository: AttributeDefinitionRepository,
    private val userRepository: UserRepository,
    private val providerProfileRepository: ProviderProfileRepository,
    private val bookingRepository: BookingRepository,
    private val reviewRepository: ReviewRepository,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val tenantConfigService: TenantConfigService
) {
    private val mapper = jacksonObjectMapper()

    // -----------------------------------------------------------------------
    // EXPORT
    // -----------------------------------------------------------------------

    @GetMapping("/{id}/export")
    @Transactional(readOnly = true)
    fun exportTenantConfig(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "false") includeData: Boolean
    ): TenantConfigExport {
        val tenant = tenantRepository.findById(id)
            .orElseThrow { NotFoundException("Tenant", id) }

        val allCategories = categoryRepository.findAllWithTranslations()
            .filter { it.tenantId == tenant.id }
        val roots = allCategories.filter { it.parent == null }.sortedBy { it.sortOrder }

        val templates = templateRepository.findAllActiveWithTranslations()
            .filter { it.tenantId == tenant.id }

        val attributes = definitionRepository.findAllActiveWithTranslations()
            .filter { it.tenantId == tenant.id }

        val categorySlugById = allCategories.associate { it.id to (it.slug ?: it.id.toString()) }
        val templateSlugById = templates.associate { it.id to it.slug }

        val runtimeData = if (includeData) {
            exportRuntimeData(tenant.id, categorySlugById, templateSlugById)
        } else null

        return TenantConfigExport(
            tenant = TenantConfigData(
                name = tenant.name,
                slug = tenant.slug,
                domain = tenant.domain,
                defaultLang = tenant.defaultLang,
                supportedLangs = tenant.supportedLangs,
                defaultCurrency = tenant.defaultCurrency,
                settings = tenant.settings
            ),
            categories = roots.map { it.toCategoryConfig(allCategories) },
            serviceTemplates = templates.map { it.toTemplateConfig(categorySlugById) },
            attributes = attributes.map { it.toAttributeConfig() },
            data = runtimeData
        )
    }

    // -----------------------------------------------------------------------
    // IMPORT (config only — runtime data is not imported)
    // -----------------------------------------------------------------------

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun importTenantConfig(@RequestBody config: TenantConfigExport): TenantImportResponse {
        return tenantConfigService.importConfig(config)
    }

    // -----------------------------------------------------------------------
    // Runtime data export helpers
    // -----------------------------------------------------------------------

    private fun exportRuntimeData(
        tenantId: UUID,
        categorySlugById: Map<UUID, String>,
        templateSlugById: Map<UUID, String>
    ): TenantRuntimeData {
        val users = userRepository.findByTenantId(tenantId)
        val providers = providerProfileRepository.findByTenantId(tenantId)
        val bookings = bookingRepository.findByTenantId(tenantId)
        val reviews = reviewRepository.findByTenantId(tenantId)
        val conversations = conversationRepository.findByTenantId(tenantId)

        return TenantRuntimeData(
            users = users.filter { it.role != UserRole.ADMIN && it.role != UserRole.SUPER_ADMIN }
                .map { u ->
                    UserExportData(
                        id = u.id,
                        email = u.email,
                        role = u.role.name,
                        phone = u.phone,
                        preferredLang = u.preferredLang,
                        isActive = u.isActive,
                        firstName = u.profile?.firstName,
                        lastName = u.profile?.lastName,
                        bio = u.profile?.bio,
                        createdAt = u.createdAt
                    )
                },
            providers = providers.map { p ->
                ProviderExportData(
                    userId = p.userId!!,
                    email = p.user.email,
                    businessName = p.businessName,
                    description = p.description,
                    addressLine = p.addressLine,
                    latitude = p.latitude,
                    longitude = p.longitude,
                    isMobile = p.isMobile,
                    isVerified = p.isVerified,
                    avgRating = p.avgRating,
                    reviewCount = p.reviewCount,
                    attributes = p.attributes,
                    categorySlugs = p.categories.mapNotNull { c ->
                        categorySlugById[c.id]
                    },
                    translations = p.translations.map { t ->
                        ProviderTranslationExportData(
                            lang = t.lang,
                            businessName = t.businessName,
                            description = t.description
                        )
                    },
                    services = p.services.map { s ->
                        ServiceExportData(
                            categorySlug = categorySlugById[s.category.id],
                            templateSlug = s.template?.let { templateSlugById[it.id] },
                            pricingType = s.pricingType.name,
                            priceAmount = s.priceAmount,
                            priceCurrency = s.priceCurrency,
                            durationMinutes = s.durationMinutes,
                            isActive = s.isActive,
                            translations = s.translations.map { t ->
                                ServiceTranslationExportData(
                                    lang = t.lang, title = t.title, description = t.description
                                )
                            },
                            createdAt = s.createdAt
                        )
                    },
                    galleryImages = p.media.map { img ->
                        GalleryImageExportData(
                            url = img.url,
                            caption = img.caption,
                            sortOrder = img.sortOrder
                        )
                    },
                    createdAt = p.createdAt
                )
            },
            bookings = bookings.map { b ->
                BookingExportData(
                    id = b.id,
                    clientEmail = b.client.email,
                    providerEmail = b.provider.user.email,
                    status = b.status.name,
                    bookingType = b.bookingType.name,
                    scheduledAt = b.scheduledAt,
                    durationMinutes = b.durationMinutes,
                    priceAmount = b.priceAmount,
                    priceCurrency = b.priceCurrency,
                    clientNotes = b.clientNotes,
                    providerNotes = b.providerNotes,
                    services = b.bookingServices.map { bs ->
                        BookingServiceExportData(
                            serviceTitle = bs.serviceTitle,
                            quantity = bs.quantity,
                            unitPrice = bs.unitPrice,
                            lineTotal = bs.lineTotal,
                            durationMinutes = bs.durationMinutes
                        )
                    },
                    createdAt = b.createdAt
                )
            },
            reviews = reviews.map { r ->
                ReviewExportData(
                    reviewerEmail = r.reviewer.email,
                    revieweeEmail = r.reviewee.email,
                    direction = r.direction.name,
                    rating = r.rating,
                    comment = r.comment,
                    isVisible = r.isVisible,
                    createdAt = r.createdAt
                )
            },
            conversations = conversations.map { c ->
                val messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(c.id)
                ConversationExportData(
                    participant1Email = c.participant1.email,
                    participant2Email = c.participant2.email,
                    conversationType = c.conversationType.name,
                    topic = c.topic,
                    messages = messages.map { m ->
                        MessageExportData(
                            senderEmail = m.sender.email,
                            content = m.content,
                            createdAt = m.createdAt
                        )
                    },
                    createdAt = c.createdAt
                )
            }
        )
    }

    // -----------------------------------------------------------------------
    // Config mapping helpers (export)
    // -----------------------------------------------------------------------

    private fun ServiceCategory.toCategoryConfig(all: List<ServiceCategory>): CategoryConfigData {
        val myChildren = all.filter { it.parent?.id == this.id }.sortedBy { it.sortOrder }
        return CategoryConfigData(
            slug = slug, icon = icon, imageUrl = imageUrl, sortOrder = sortOrder,
            translations = translations.map {
                CategoryTranslationEntry(lang = it.lang, name = it.name, description = it.description)
            },
            children = myChildren.map { it.toCategoryConfig(all) }
        )
    }

    private fun ServiceTemplate.toTemplateConfig(categorySlugById: Map<UUID, String>) =
        ServiceTemplateConfigData(
            slug = slug,
            categorySlug = categorySlugById[category.id] ?: category.id.toString(),
            defaultDurationMinutes = defaultDurationMinutes,
            sortOrder = sortOrder,
            translations = translations.map {
                TemplateTranslationEntry(lang = it.lang, title = it.title, description = it.description)
            }
        )

    private fun AttributeDefinition.toAttributeConfig(): AttributeConfigData {
        return AttributeConfigData(
            domain = domain, key = key, dataType = dataType.name,
            isRequired = isRequired,
            options = options?.let { mapper.readValue(it) },
            validation = validation?.let { mapper.readValue(it) },
            sortOrder = sortOrder,
            translations = translations.map { t ->
                AttributeTranslationEntry(
                    lang = t.lang, label = t.label, hint = t.hint,
                    optionLabels = t.optionLabels?.let { mapper.readValue(it) }
                )
            }
        )
    }
}
