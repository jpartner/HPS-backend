package com.hps.api.provider

import com.hps.common.dto.PageResponse
import com.hps.common.i18n.LanguageContext
import com.hps.persistence.service.ServiceCategoryRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class ProviderController(
    private val providerService: ProviderService,
    private val categoryRepository: ServiceCategoryRepository
) {
    @GetMapping("/providers")
    fun listProviders(
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) categorySlug: String?,
        @RequestParam(required = false) countryCode: String?,
        @RequestParam(required = false) regionId: UUID?,
        @RequestParam(required = false) cityId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageResponse<ProviderSummaryDto> {
        val lang = LanguageContext.get().code
        val resolvedCategoryId = categoryId ?: categorySlug?.let { slug ->
            categoryRepository.findBySlug(slug)?.id
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found for slug: $slug")
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Either categoryId or categorySlug is required")
        return providerService.listByCategory(resolvedCategoryId, countryCode, regionId, cityId, page, size, lang)
    }

    @GetMapping("/providers/{id}")
    fun getProvider(@PathVariable id: UUID): ProviderDetailDto {
        val lang = LanguageContext.get().code
        return providerService.getDetail(id, lang)
    }

    @PostMapping("/providers/me")
    @ResponseStatus(HttpStatus.CREATED)
    fun createProviderProfile(
        @Valid @RequestBody request: CreateProviderRequest,
        auth: Authentication
    ): ProviderDetailDto {
        val userId = UUID.fromString(auth.principal as String)
        return providerService.createProfile(userId, request)
    }

    @PutMapping("/providers/me")
    fun updateProviderProfile(
        @Valid @RequestBody request: UpdateProviderRequest,
        auth: Authentication
    ): ProviderDetailDto {
        val userId = UUID.fromString(auth.principal as String)
        return providerService.updateProfile(userId, request)
    }
}
