package com.hps.api.attribute

import com.hps.api.auth.isAdmin
import com.hps.api.auth.userId
import com.hps.common.exception.ForbiddenException
import com.hps.common.i18n.LanguageContext
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class AttributeController(
    private val attributeService: AttributeService
) {
    // === Public: get attribute definitions for a domain ===

    @GetMapping("/attributes")
    fun listDefinitions(
        @RequestParam(required = false) domain: String?
    ): List<AttributeDefinitionDto> {
        val lang = LanguageContext.get().code
        return attributeService.listDefinitions(domain, lang)
    }

    // === Admin: manage attribute definitions ===

    @PostMapping("/admin/attributes")
    @ResponseStatus(HttpStatus.CREATED)
    fun createDefinition(
        @Valid @RequestBody request: CreateAttributeDefinitionRequest,
        auth: Authentication
    ): AttributeDefinitionDto {
        if (!auth.isAdmin()) throw ForbiddenException("Admin only")
        return attributeService.createDefinition(request)
    }

    @PutMapping("/admin/attributes/{id}")
    fun updateDefinition(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateAttributeDefinitionRequest,
        auth: Authentication
    ): AttributeDefinitionDto {
        if (!auth.isAdmin()) throw ForbiddenException("Admin only")
        return attributeService.updateDefinition(id, request)
    }

    // === Provider: manage own attributes ===

    @GetMapping("/providers/{providerId}/attributes")
    fun getProviderAttributes(@PathVariable providerId: UUID): List<ProviderAttributeWithDefinition> {
        val lang = LanguageContext.get().code
        return attributeService.getProviderAttributesWithDefinitions(providerId, lang)
    }

    @PutMapping("/providers/me/attributes")
    fun setMyAttributes(
        @RequestBody values: Map<String, Any?>,
        auth: Authentication
    ): Map<String, Any?> {
        return attributeService.setProviderAttributes(auth.userId(), values)
    }
}
