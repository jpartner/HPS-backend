package com.hps.api.admin

import com.hps.common.exception.NotFoundException
import com.hps.common.tenant.TenantContext
import com.hps.domain.service.ServiceCategory
import com.hps.domain.service.ServiceCategoryTranslation
import com.hps.persistence.service.ServiceCategoryRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/categories")
@Transactional(readOnly = true)
class AdminCategoryController(
    private val categoryRepository: ServiceCategoryRepository
) {

    @GetMapping
    fun listCategories(): List<AdminCategoryDto> {
        val tenantId = TenantContext.require()
        val all = categoryRepository.findAllWithTranslations()
            .filter { it.tenantId == tenantId }
        val roots = all.filter { it.parent == null }.sortedBy { it.sortOrder }
        return roots.map { it.toAdminDto(all) }
    }

    @GetMapping("/{id}")
    fun getCategory(@PathVariable id: UUID): AdminCategoryDto {
        val tenantId = TenantContext.require()
        val category = categoryRepository.findById(id)
            .orElseThrow { NotFoundException("Category", id) }
        if (category.tenantId != tenantId) throw NotFoundException("Category", id)
        val all = categoryRepository.findAllWithTranslations()
            .filter { it.tenantId == tenantId }
        return category.toAdminDto(all)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun createCategory(@Valid @RequestBody request: CreateCategoryRequest): AdminCategoryDto {
        val tenantId = TenantContext.require()

        val parent = request.parentId?.let { parentId ->
            categoryRepository.findById(parentId)
                .orElseThrow { NotFoundException("Parent category", parentId) }
                .also { if (it.tenantId != tenantId) throw NotFoundException("Parent category", parentId) }
        }

        val category = ServiceCategory(
            tenantId = tenantId,
            parent = parent,
            slug = request.slug,
            icon = request.icon,
            imageUrl = request.imageUrl,
            sortOrder = request.sortOrder
        )

        request.translations.forEach { t ->
            category.translations.add(
                ServiceCategoryTranslation(
                    category = category,
                    lang = t.lang,
                    name = t.name,
                    description = t.description
                )
            )
        }

        categoryRepository.save(category)
        return category.toAdminDto(emptyList())
    }

    @PutMapping("/{id}")
    @Transactional
    fun updateCategory(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateCategoryRequest
    ): AdminCategoryDto {
        val tenantId = TenantContext.require()
        val category = categoryRepository.findById(id)
            .orElseThrow { NotFoundException("Category", id) }
        if (category.tenantId != tenantId) throw NotFoundException("Category", id)

        request.slug?.let { category.slug = it }
        request.icon?.let { category.icon = it }
        request.imageUrl?.let { category.imageUrl = it }
        request.sortOrder?.let { category.sortOrder = it }

        if (request.translations != null) {
            category.translations.clear()
            categoryRepository.saveAndFlush(category)
            request.translations.forEach { t ->
                category.translations.add(
                    ServiceCategoryTranslation(
                        category = category,
                        lang = t.lang,
                        name = t.name,
                        description = t.description
                    )
                )
            }
        }

        categoryRepository.save(category)
        val all = categoryRepository.findAllWithTranslations()
            .filter { it.tenantId == tenantId }
        return category.toAdminDto(all)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun deleteCategory(@PathVariable id: UUID) {
        val tenantId = TenantContext.require()
        val category = categoryRepository.findById(id)
            .orElseThrow { NotFoundException("Category", id) }
        if (category.tenantId != tenantId) throw NotFoundException("Category", id)
        categoryRepository.delete(category)
    }

    @PutMapping("/reorder")
    @Transactional
    fun reorderCategories(@RequestBody items: List<ReorderItem>) {
        val tenantId = TenantContext.require()
        items.forEach { item ->
            val category = categoryRepository.findById(item.id)
                .orElseThrow { NotFoundException("Category", item.id) }
            if (category.tenantId != tenantId) throw NotFoundException("Category", item.id)
            category.sortOrder = item.sortOrder
        }
        categoryRepository.flush()
    }

    private fun ServiceCategory.toAdminDto(all: List<ServiceCategory>): AdminCategoryDto {
        val myChildren = all.filter { it.parent?.id == this.id }.sortedBy { it.sortOrder }
        return AdminCategoryDto(
            id = id,
            slug = slug,
            icon = icon,
            imageUrl = imageUrl,
            sortOrder = sortOrder,
            parentId = parent?.id,
            translations = translations.map { CategoryTranslationEntry(lang = it.lang, name = it.name, description = it.description) },
            children = myChildren.map { it.toAdminDto(all) }
        )
    }
}

// --- DTOs ---

data class AdminCategoryDto(
    val id: UUID,
    val slug: String?,
    val icon: String?,
    val imageUrl: String?,
    val sortOrder: Int,
    val parentId: UUID?,
    val translations: List<CategoryTranslationEntry>,
    val children: List<AdminCategoryDto>
)

data class CategoryTranslationEntry(
    val lang: String,
    val name: String,
    val description: String? = null
)

data class CreateCategoryRequest(
    val slug: String? = null,
    val icon: String? = null,
    val imageUrl: String? = null,
    val sortOrder: Int = 0,
    val parentId: UUID? = null,
    val translations: List<CategoryTranslationEntry> = emptyList()
)

data class UpdateCategoryRequest(
    val slug: String? = null,
    val icon: String? = null,
    val imageUrl: String? = null,
    val sortOrder: Int? = null,
    val translations: List<CategoryTranslationEntry>? = null
)

data class ReorderItem(
    val id: UUID,
    val sortOrder: Int
)
