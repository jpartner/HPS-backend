package com.hps.api.service

import com.hps.common.i18n.LanguageContext
import com.hps.common.i18n.bestTranslation
import com.hps.persistence.service.ServiceTemplateRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/service-templates")
@Transactional(readOnly = true)
class ServiceTemplateController(
    private val templateRepository: ServiceTemplateRepository
) {
    @GetMapping
    fun listAll(): List<ServiceTemplateDto> {
        val lang = LanguageContext.get().code
        return templateRepository.findAllActiveWithTranslations()
            .map { it.toDto(lang) }
    }

    @GetMapping(params = ["categoryId"])
    fun listByCategory(@RequestParam categoryId: UUID): List<ServiceTemplateDto> {
        val lang = LanguageContext.get().code
        return templateRepository.findByCategoryWithTranslations(categoryId)
            .map { it.toDto(lang) }
    }

    private fun com.hps.domain.service.ServiceTemplate.toDto(lang: String) = ServiceTemplateDto(
        id = id,
        slug = slug,
        categoryId = category.id,
        categoryName = category.translations.bestTranslation(lang, { it.lang }, { it.name }),
        title = translations.bestTranslation(lang, { it.lang }, { it.title }),
        description = translations.firstOrNull { it.lang == lang }?.description
            ?: translations.firstOrNull { it.lang == "en" }?.description,
        defaultDurationMinutes = defaultDurationMinutes,
        sortOrder = sortOrder
    )
}

data class ServiceTemplateDto(
    val id: UUID,
    val slug: String,
    val categoryId: UUID,
    val categoryName: String,
    val title: String,
    val description: String?,
    val defaultDurationMinutes: Int?,
    val sortOrder: Int
)
