package com.hps.api.admin

import com.hps.common.exception.NotFoundException
import com.hps.common.tenant.TenantContext
import com.hps.domain.service.ServiceTemplate
import com.hps.domain.service.ServiceTemplateTranslation
import com.hps.persistence.service.ServiceCategoryRepository
import com.hps.persistence.service.ServiceTemplateRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/service-templates")
@Transactional(readOnly = true)
class AdminTemplateController(
    private val templateRepository: ServiceTemplateRepository,
    private val categoryRepository: ServiceCategoryRepository
) {

    @GetMapping
    fun listTemplates(): List<AdminTemplateDto> {
        val tenantId = TenantContext.require()
        return templateRepository.findAllActiveWithTranslations()
            .filter { it.tenantId == tenantId }
            .map { it.toAdminDto() }
    }

    @GetMapping("/{id}")
    fun getTemplate(@PathVariable id: UUID): AdminTemplateDto {
        val tenantId = TenantContext.require()
        val template = templateRepository.findById(id)
            .orElseThrow { NotFoundException("ServiceTemplate", id) }
        if (template.tenantId != tenantId) throw NotFoundException("ServiceTemplate", id)
        return template.toAdminDto()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun createTemplate(@Valid @RequestBody request: CreateTemplateRequest): AdminTemplateDto {
        val tenantId = TenantContext.require()

        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { NotFoundException("Category", request.categoryId) }
        if (category.tenantId != tenantId) throw NotFoundException("Category", request.categoryId)

        val template = ServiceTemplate(
            tenantId = tenantId,
            category = category,
            slug = request.slug,
            defaultDurationMinutes = request.defaultDurationMinutes,
            sortOrder = request.sortOrder
        )

        request.translations.forEach { t ->
            template.translations.add(
                ServiceTemplateTranslation(
                    template = template,
                    lang = t.lang,
                    title = t.title,
                    description = t.description
                )
            )
        }

        templateRepository.save(template)
        return template.toAdminDto()
    }

    @PutMapping("/{id}")
    @Transactional
    fun updateTemplate(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateTemplateRequest
    ): AdminTemplateDto {
        val tenantId = TenantContext.require()
        val template = templateRepository.findById(id)
            .orElseThrow { NotFoundException("ServiceTemplate", id) }
        if (template.tenantId != tenantId) throw NotFoundException("ServiceTemplate", id)

        request.defaultDurationMinutes?.let { template.defaultDurationMinutes = it }
        request.sortOrder?.let { template.sortOrder = it }
        request.isActive?.let { template.isActive = it }

        if (request.translations != null) {
            template.translations.clear()
            templateRepository.saveAndFlush(template)
            request.translations.forEach { t ->
                template.translations.add(
                    ServiceTemplateTranslation(
                        template = template,
                        lang = t.lang,
                        title = t.title,
                        description = t.description
                    )
                )
            }
        }

        templateRepository.save(template)
        return template.toAdminDto()
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun deleteTemplate(@PathVariable id: UUID) {
        val tenantId = TenantContext.require()
        val template = templateRepository.findById(id)
            .orElseThrow { NotFoundException("ServiceTemplate", id) }
        if (template.tenantId != tenantId) throw NotFoundException("ServiceTemplate", id)
        // Soft-delete by deactivating
        template.isActive = false
        templateRepository.save(template)
    }

    private fun ServiceTemplate.toAdminDto() = AdminTemplateDto(
        id = id,
        slug = slug,
        categoryId = category.id,
        defaultDurationMinutes = defaultDurationMinutes,
        sortOrder = sortOrder,
        isActive = isActive,
        translations = translations.map {
            TemplateTranslationEntry(lang = it.lang, title = it.title, description = it.description)
        }
    )
}

// --- DTOs ---

data class AdminTemplateDto(
    val id: UUID,
    val slug: String,
    val categoryId: UUID,
    val defaultDurationMinutes: Int?,
    val sortOrder: Int,
    val isActive: Boolean,
    val translations: List<TemplateTranslationEntry>
)

data class TemplateTranslationEntry(
    val lang: String,
    val title: String,
    val description: String? = null
)

data class CreateTemplateRequest(
    val slug: String,
    val categoryId: UUID,
    val defaultDurationMinutes: Int? = null,
    val sortOrder: Int = 0,
    val translations: List<TemplateTranslationEntry> = emptyList()
)

data class UpdateTemplateRequest(
    val defaultDurationMinutes: Int? = null,
    val sortOrder: Int? = null,
    val isActive: Boolean? = null,
    val translations: List<TemplateTranslationEntry>? = null
)
