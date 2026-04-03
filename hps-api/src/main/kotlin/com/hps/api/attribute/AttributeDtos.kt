package com.hps.api.attribute

import java.util.UUID

data class AttributeDefinitionDto(
    val id: UUID,
    val domain: String,
    val key: String,
    val dataType: String,
    val isRequired: Boolean,
    val options: List<String>?,
    val validation: ValidationRules?,
    val label: String,
    val hint: String?,
    val optionLabels: Map<String, String>?,
    val sortOrder: Int
)

data class ValidationRules(
    val min: Double? = null,
    val max: Double? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null
)

data class CreateAttributeDefinitionRequest(
    val domain: String,
    val key: String,
    val dataType: String = "TEXT",
    val isRequired: Boolean = false,
    val options: List<String>? = null,
    val validation: ValidationRules? = null,
    val sortOrder: Int = 0,
    val translations: List<AttributeTranslationRequest> = emptyList()
)

data class AttributeTranslationRequest(
    val lang: String,
    val label: String,
    val hint: String? = null,
    val optionLabels: Map<String, String>? = null
)

data class UpdateAttributeDefinitionRequest(
    val isRequired: Boolean? = null,
    val options: List<String>? = null,
    val validation: ValidationRules? = null,
    val sortOrder: Int? = null,
    val isActive: Boolean? = null,
    val translations: List<AttributeTranslationRequest>? = null
)

data class ProviderAttributeValue(
    val key: String,
    val value: Any?
)
