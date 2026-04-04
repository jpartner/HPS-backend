package com.hps.api.attribute

import com.hps.api.auth.userId
import com.hps.common.i18n.LanguageContext
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class AttributeController(
    private val attributeService: AttributeService
) {
    @GetMapping("/attributes")
    fun listDefinitions(
        @RequestParam(required = false) domain: String?
    ): List<AttributeDefinitionDto> {
        val lang = LanguageContext.get().code
        return attributeService.listDefinitions(domain, lang)
    }

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
