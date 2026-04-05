package com.hps.api.admin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hps.common.exception.BadRequestException
import com.hps.domain.attribute.AttributeDataType
import com.hps.domain.attribute.AttributeDefinition
import com.hps.domain.attribute.AttributeDefinitionTranslation
import com.hps.domain.service.ServiceCategory
import com.hps.domain.service.ServiceCategoryTranslation
import com.hps.domain.service.ServiceTemplate
import com.hps.domain.service.ServiceTemplateTranslation
import com.hps.domain.tenant.Tenant
import com.hps.domain.tenant.TenantAdmin
import com.hps.domain.user.User
import com.hps.domain.user.UserProfile
import com.hps.domain.user.UserRole
import com.hps.persistence.attribute.AttributeDefinitionRepository
import com.hps.persistence.service.ServiceCategoryRepository
import com.hps.persistence.service.ServiceTemplateRepository
import com.hps.persistence.tenant.TenantAdminRepository
import com.hps.persistence.tenant.TenantRepository
import com.hps.persistence.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TenantConfigService(
    private val tenantRepository: TenantRepository,
    private val categoryRepository: ServiceCategoryRepository,
    private val templateRepository: ServiceTemplateRepository,
    private val definitionRepository: AttributeDefinitionRepository,
    private val userRepository: UserRepository,
    private val tenantAdminRepository: TenantAdminRepository,
    private val passwordEncoder: PasswordEncoder
) {
    private val log = LoggerFactory.getLogger(TenantConfigService::class.java)
    private val mapper = jacksonObjectMapper()

    @Transactional
    fun importConfig(config: TenantConfigExport): TenantImportResponse {
        if (tenantRepository.findBySlug(config.tenant.slug) != null) {
            throw BadRequestException("Tenant with slug '${config.tenant.slug}' already exists")
        }

        // 1. Create tenant
        val tenant = tenantRepository.save(
            Tenant(
                name = config.tenant.name,
                slug = config.tenant.slug,
                domain = config.tenant.domain,
                defaultLang = config.tenant.defaultLang,
                supportedLangs = config.tenant.supportedLangs,
                defaultCurrency = config.tenant.defaultCurrency,
                settings = config.tenant.settings
            )
        )
        log.info("Created tenant '{}' ({})", tenant.name, tenant.id)

        // 2. Create categories (preserving hierarchy)
        val categoryBySlug = mutableMapOf<String, ServiceCategory>()
        var categoriesCreated = 0
        for (root in config.categories) {
            categoriesCreated += importCategory(root, null, tenant.id, categoryBySlug)
        }
        log.info("Created {} categories", categoriesCreated)

        // 3. Create service templates
        var templatesCreated = 0
        for (tmpl in config.serviceTemplates) {
            val category = categoryBySlug[tmpl.categorySlug]
                ?: throw BadRequestException(
                    "Template '${tmpl.slug}' references unknown category slug '${tmpl.categorySlug}'"
                )
            val template = ServiceTemplate(
                tenantId = tenant.id,
                category = category,
                slug = tmpl.slug,
                defaultDurationMinutes = tmpl.defaultDurationMinutes,
                sortOrder = tmpl.sortOrder
            )
            tmpl.translations.forEach { t ->
                template.translations.add(
                    ServiceTemplateTranslation(
                        template = template, lang = t.lang,
                        title = t.title, description = t.description
                    )
                )
            }
            templateRepository.save(template)
            templatesCreated++
        }
        log.info("Created {} service templates", templatesCreated)

        // 4. Create attribute definitions
        var attributesCreated = 0
        for (attr in config.attributes) {
            val def = AttributeDefinition(
                tenantId = tenant.id,
                domain = attr.domain,
                key = attr.key,
                dataType = AttributeDataType.valueOf(attr.dataType),
                isRequired = attr.isRequired,
                options = attr.options?.let { mapper.writeValueAsString(it) },
                validation = attr.validation?.let { mapper.writeValueAsString(it) },
                sortOrder = attr.sortOrder
            )
            attr.translations.forEach { t ->
                def.translations.add(
                    AttributeDefinitionTranslation(
                        attribute = def, lang = t.lang, label = t.label,
                        hint = t.hint,
                        optionLabels = t.optionLabels?.let { mapper.writeValueAsString(it) }
                    )
                )
            }
            definitionRepository.save(def)
            attributesCreated++
        }
        log.info("Created {} attribute definitions", attributesCreated)

        // 5. Create admin user if credentials provided
        var adminCreated = false
        val creds = config.admin
        if (creds != null) {
            if (userRepository.existsByEmailAndTenantId(creds.email, tenant.id)) {
                throw BadRequestException("User '${creds.email}' already exists for this tenant")
            }
            val user = User(
                email = creds.email,
                tenantId = tenant.id,
                passwordHash = passwordEncoder.encode(creds.password),
                role = UserRole.ADMIN
            )
            user.profile = UserProfile(
                user = user,
                firstName = creds.firstName,
                lastName = creds.lastName
            )
            userRepository.save(user)
            tenantAdminRepository.save(TenantAdmin(tenantId = tenant.id, userId = user.id))
            adminCreated = true
            log.info("Created admin user: {}", creds.email)
        }

        return TenantImportResponse(
            tenantId = tenant.id,
            slug = tenant.slug,
            categoriesCreated = categoriesCreated,
            templatesCreated = templatesCreated,
            attributesCreated = attributesCreated,
            adminCreated = adminCreated
        )
    }

    private fun importCategory(
        data: CategoryConfigData,
        parent: ServiceCategory?,
        tenantId: java.util.UUID,
        slugIndex: MutableMap<String, ServiceCategory>
    ): Int {
        val category = ServiceCategory(
            tenantId = tenantId,
            parent = parent,
            slug = data.slug,
            icon = data.icon,
            imageUrl = data.imageUrl,
            sortOrder = data.sortOrder
        )
        data.translations.forEach { t ->
            category.translations.add(
                ServiceCategoryTranslation(
                    category = category, lang = t.lang,
                    name = t.name, description = t.description
                )
            )
        }
        categoryRepository.save(category)

        val slug = data.slug ?: category.id.toString()
        slugIndex[slug] = category

        var count = 1
        for (child in data.children) {
            count += importCategory(child, category, tenantId, slugIndex)
        }
        return count
    }
}
