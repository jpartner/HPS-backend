package com.hps.api.admin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hps.common.exception.NotFoundException
import com.hps.common.tenant.TenantContext
import com.hps.domain.attribute.AttributeDataType
import com.hps.domain.attribute.AttributeDefinition
import com.hps.domain.attribute.AttributeDefinitionTranslation
import com.hps.persistence.attribute.AttributeDefinitionRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/attributes")
@Transactional(readOnly = true)
class AdminAttributeController(
    private val definitionRepository: AttributeDefinitionRepository
) {
    private val mapper = jacksonObjectMapper()

    @GetMapping
    fun listDefinitions(
        @RequestParam(required = false) domain: String?
    ): List<AdminAttributeDefinitionDto> {
        val tenantId = TenantContext.require()
        val defs = if (domain != null) {
            definitionRepository.findByDomainWithTranslations(domain)
        } else {
            definitionRepository.findAllActiveWithTranslations()
        }
        return defs.filter { it.tenantId == tenantId }.map { it.toAdminDto() }
    }

    @GetMapping("/{id}")
    fun getDefinition(@PathVariable id: UUID): AdminAttributeDefinitionDto {
        val tenantId = TenantContext.require()
        val def = definitionRepository.findById(id)
            .orElseThrow { NotFoundException("AttributeDefinition", id) }
        if (def.tenantId != tenantId) throw NotFoundException("AttributeDefinition", id)
        return def.toAdminDto()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun createDefinition(
        @Valid @RequestBody request: CreateAdminAttributeRequest
    ): AdminAttributeDefinitionDto {
        val tenantId = TenantContext.require()

        val def = AttributeDefinition(
            tenantId = tenantId,
            domain = request.domain,
            key = request.key,
            dataType = AttributeDataType.valueOf(request.dataType),
            isRequired = request.isRequired,
            options = request.options?.let { mapper.writeValueAsString(it) },
            validation = request.validation?.let { mapper.writeValueAsString(it) },
            sortOrder = request.sortOrder
        )

        request.translations.forEach { t ->
            def.translations.add(
                AttributeDefinitionTranslation(
                    attribute = def,
                    lang = t.lang,
                    label = t.label,
                    hint = t.hint,
                    optionLabels = t.optionLabels?.let { mapper.writeValueAsString(it) }
                )
            )
        }

        definitionRepository.save(def)
        return def.toAdminDto()
    }

    @PutMapping("/{id}")
    @Transactional
    fun updateDefinition(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateAdminAttributeRequest
    ): AdminAttributeDefinitionDto {
        val tenantId = TenantContext.require()
        val def = definitionRepository.findById(id)
            .orElseThrow { NotFoundException("AttributeDefinition", id) }
        if (def.tenantId != tenantId) throw NotFoundException("AttributeDefinition", id)

        request.dataType?.let { def.dataType = AttributeDataType.valueOf(it) }
        request.isRequired?.let { def.isRequired = it }
        request.options?.let { def.options = mapper.writeValueAsString(it) }
        request.validation?.let { def.validation = mapper.writeValueAsString(it) }
        request.sortOrder?.let { def.sortOrder = it }
        request.isActive?.let { def.isActive = it }

        if (request.translations != null) {
            def.translations.clear()
            definitionRepository.saveAndFlush(def)
            request.translations.forEach { t ->
                def.translations.add(
                    AttributeDefinitionTranslation(
                        attribute = def,
                        lang = t.lang,
                        label = t.label,
                        hint = t.hint,
                        optionLabels = t.optionLabels?.let { mapper.writeValueAsString(it) }
                    )
                )
            }
        }

        definitionRepository.save(def)
        return def.toAdminDto()
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun deleteDefinition(@PathVariable id: UUID) {
        val tenantId = TenantContext.require()
        val def = definitionRepository.findById(id)
            .orElseThrow { NotFoundException("AttributeDefinition", id) }
        if (def.tenantId != tenantId) throw NotFoundException("AttributeDefinition", id)
        // Soft-delete by deactivating
        def.isActive = false
        definitionRepository.save(def)
    }

    private fun AttributeDefinition.toAdminDto(): AdminAttributeDefinitionDto {
        return AdminAttributeDefinitionDto(
            id = id,
            domain = domain,
            key = key,
            dataType = dataType.name,
            isRequired = isRequired,
            options = options?.let { mapper.readValue(it) },
            validation = validation?.let { mapper.readValue(it) },
            sortOrder = sortOrder,
            isActive = isActive,
            translations = translations.map { t ->
                AttributeTranslationEntry(
                    lang = t.lang,
                    label = t.label,
                    hint = t.hint,
                    optionLabels = t.optionLabels?.let { mapper.readValue(it) }
                )
            }
        )
    }
}

// --- DTOs ---

data class AdminAttributeDefinitionDto(
    val id: UUID,
    val domain: String,
    val key: String,
    val dataType: String,
    val isRequired: Boolean,
    val options: List<String>?,
    val validation: AdminValidationRules?,
    val sortOrder: Int,
    val isActive: Boolean,
    val translations: List<AttributeTranslationEntry>
)

data class AdminValidationRules(
    val min: Double? = null,
    val max: Double? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null
)

data class AttributeTranslationEntry(
    val lang: String,
    val label: String,
    val hint: String? = null,
    val optionLabels: Map<String, String>? = null
)

data class CreateAdminAttributeRequest(
    val domain: String,
    val key: String,
    val dataType: String = "TEXT",
    val isRequired: Boolean = false,
    val options: List<String>? = null,
    val validation: AdminValidationRules? = null,
    val sortOrder: Int = 0,
    val translations: List<AttributeTranslationEntry> = emptyList()
)

data class UpdateAdminAttributeRequest(
    val dataType: String? = null,
    val isRequired: Boolean? = null,
    val options: List<String>? = null,
    val validation: AdminValidationRules? = null,
    val sortOrder: Int? = null,
    val isActive: Boolean? = null,
    val translations: List<AttributeTranslationEntry>? = null
)
