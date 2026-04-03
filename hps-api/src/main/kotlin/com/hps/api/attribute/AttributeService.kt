package com.hps.api.attribute

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hps.common.exception.BadRequestException
import com.hps.common.exception.NotFoundException
import com.hps.common.i18n.bestTranslation
import com.hps.domain.attribute.AttributeDataType
import com.hps.domain.attribute.AttributeDefinition
import com.hps.domain.attribute.AttributeDefinitionTranslation
import com.hps.persistence.attribute.AttributeDefinitionRepository
import com.hps.persistence.user.ProviderProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AttributeService(
    private val definitionRepository: AttributeDefinitionRepository,
    private val providerRepository: ProviderProfileRepository
) {
    private val mapper = jacksonObjectMapper()

    // === Definitions (Admin) ===

    fun listDefinitions(domain: String?, lang: String): List<AttributeDefinitionDto> {
        val defs = if (domain != null) {
            definitionRepository.findByDomainWithTranslations(domain)
        } else {
            definitionRepository.findAllActiveWithTranslations()
        }
        return defs.map { it.toDto(lang) }
    }

    @Transactional
    fun createDefinition(request: CreateAttributeDefinitionRequest): AttributeDefinitionDto {
        val def = AttributeDefinition(
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
        return def.toDto("en")
    }

    @Transactional
    fun updateDefinition(id: UUID, request: UpdateAttributeDefinitionRequest): AttributeDefinitionDto {
        val def = definitionRepository.findById(id)
            .orElseThrow { NotFoundException("AttributeDefinition", id) }

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
        return def.toDto("en")
    }

    // === Provider Attributes ===

    fun getProviderAttributes(providerId: UUID): Map<String, Any?> {
        val provider = providerRepository.findById(providerId)
            .orElseThrow { NotFoundException("Provider", providerId) }
        return mapper.readValue(provider.attributes)
    }

    @Transactional
    fun setProviderAttributes(providerId: UUID, values: Map<String, Any?>): Map<String, Any?> {
        val provider = providerRepository.findById(providerId)
            .orElseThrow { NotFoundException("Provider", providerId) }

        // Merge with existing
        val existing: MutableMap<String, Any?> = mapper.readValue(provider.attributes)
        existing.putAll(values)

        // Validate against definitions for all domains the provider belongs to
        val domains = provider.categories.mapNotNull { cat ->
            cat.translations.firstOrNull { it.lang == "en" }?.name
        }

        val allDefs = domains.flatMap { definitionRepository.findByDomainWithTranslations(it) }
        validateAttributes(existing, allDefs)

        provider.attributes = mapper.writeValueAsString(existing)
        provider.updatedAt = Instant.now()
        providerRepository.save(provider)

        return existing
    }

    fun getProviderAttributesWithDefinitions(providerId: UUID, lang: String): List<ProviderAttributeWithDefinition> {
        val provider = providerRepository.findById(providerId)
            .orElseThrow { NotFoundException("Provider", providerId) }

        val values: Map<String, Any?> = mapper.readValue(provider.attributes)
        if (values.isEmpty()) return emptyList()

        val domains = provider.categories.mapNotNull { cat ->
            cat.translations.firstOrNull { it.lang == "en" }?.name
        }

        val defs = domains.flatMap { definitionRepository.findByDomainWithTranslations(it) }
            .distinctBy { it.key }
        val defsByKey = defs.associateBy { it.key }

        // Return all attributes — those with definitions get rich metadata,
        // those without get a simple label derived from the key
        return values.filter { it.value != null }.map { (key, value) ->
            val def = defsByKey[key]
            ProviderAttributeWithDefinition(
                definition = if (def != null) {
                    def.toDto(lang)
                } else {
                    // Generate a simple definition for undefined keys
                    AttributeDefinitionDto(
                        id = java.util.UUID(0, 0),
                        domain = "",
                        key = key,
                        dataType = when (value) {
                            is Boolean -> "BOOLEAN"
                            is Number -> "NUMBER"
                            is List<*> -> "MULTI_SELECT"
                            else -> "TEXT"
                        },
                        isRequired = false,
                        options = null,
                        validation = null,
                        label = key.replace("_", " ").replaceFirstChar { it.uppercase() },
                        hint = null,
                        optionLabels = null,
                        sortOrder = 999
                    )
                },
                value = value
            )
        }
    }

    private fun validateAttributes(values: Map<String, Any?>, definitions: List<AttributeDefinition>) {
        for (def in definitions) {
            val value = values[def.key]

            if (def.isRequired && (value == null || value.toString().isBlank())) {
                val label = def.translations.firstOrNull { it.lang == "en" }?.label ?: def.key
                throw BadRequestException("'$label' is required")
            }

            if (value != null) {
                when (def.dataType) {
                    AttributeDataType.NUMBER -> {
                        if (value !is Number) throw BadRequestException("'${def.key}' must be a number")
                        val validation = def.validation?.let { mapper.readValue<ValidationRules>(it) }
                        if (validation?.min != null && value.toDouble() < validation.min)
                            throw BadRequestException("'${def.key}' must be at least ${validation.min}")
                        if (validation?.max != null && value.toDouble() > validation.max)
                            throw BadRequestException("'${def.key}' must be at most ${validation.max}")
                    }
                    AttributeDataType.BOOLEAN -> {
                        if (value !is Boolean) throw BadRequestException("'${def.key}' must be true or false")
                    }
                    AttributeDataType.SELECT -> {
                        val options = def.options?.let { mapper.readValue<List<String>>(it) } ?: emptyList()
                        if (options.isNotEmpty() && value.toString() !in options)
                            throw BadRequestException("'${def.key}' must be one of: ${options.joinToString()}")
                    }
                    AttributeDataType.MULTI_SELECT -> {
                        val options = def.options?.let { mapper.readValue<List<String>>(it) } ?: emptyList()
                        val selected = when (value) {
                            is List<*> -> value.map { it.toString() }
                            else -> throw BadRequestException("'${def.key}' must be a list")
                        }
                        if (options.isNotEmpty()) {
                            val invalid = selected.filter { it !in options }
                            if (invalid.isNotEmpty())
                                throw BadRequestException("'${def.key}' contains invalid values: ${invalid.joinToString()}")
                        }
                    }
                    AttributeDataType.TEXT -> {
                        val validation = def.validation?.let { mapper.readValue<ValidationRules>(it) }
                        val str = value.toString()
                        if (validation?.minLength != null && str.length < validation.minLength)
                            throw BadRequestException("'${def.key}' must be at least ${validation.minLength} characters")
                        if (validation?.maxLength != null && str.length > validation.maxLength)
                            throw BadRequestException("'${def.key}' must be at most ${validation.maxLength} characters")
                    }
                }
            }
        }
    }

    private fun AttributeDefinition.toDto(lang: String): AttributeDefinitionDto {
        val t = translations.firstOrNull { it.lang == lang }
            ?: translations.firstOrNull { it.lang == "en" }

        return AttributeDefinitionDto(
            id = id,
            domain = domain,
            key = key,
            dataType = dataType.name,
            isRequired = isRequired,
            options = options?.let { mapper.readValue(it) },
            validation = validation?.let { mapper.readValue(it) },
            label = t?.label ?: key,
            hint = t?.hint,
            optionLabels = t?.optionLabels?.let { mapper.readValue(it) },
            sortOrder = sortOrder
        )
    }
}

data class ProviderAttributeWithDefinition(
    val definition: AttributeDefinitionDto,
    val value: Any?
)
