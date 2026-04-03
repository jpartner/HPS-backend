package com.hps.api.category

import com.hps.common.i18n.LanguageContext
import com.hps.common.i18n.bestTranslation
import com.hps.domain.service.ServiceCategory
import com.hps.persistence.service.ServiceCategoryRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/categories")
class CategoryController(
    private val categoryRepository: ServiceCategoryRepository
) {
    @GetMapping
    fun listCategories(): List<CategoryDto> {
        val lang = LanguageContext.get().code
        val all = categoryRepository.findAllWithTranslations()
        val roots = all.filter { it.parent == null }.sortedBy { it.sortOrder }
        return roots.map { it.toDto(lang, all) }
    }

    private fun ServiceCategory.toDto(lang: String, all: List<ServiceCategory>): CategoryDto {
        val myChildren = all.filter { it.parent?.id == this.id }.sortedBy { it.sortOrder }
        return CategoryDto(
            id = id,
            name = translations.bestTranslation(lang, { it.lang }, { it.name }),
            icon = icon,
            children = myChildren.map { it.toDto(lang, all) }
        )
    }
}
