package com.hps.api.admin

import com.hps.common.exception.NotFoundException
import com.hps.common.tenant.TenantContext
import com.hps.persistence.tenant.TenantRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/languages")
class AdminLanguageController(
    private val tenantRepository: TenantRepository
) {

    @GetMapping
    @Transactional(readOnly = true)
    fun getLanguages(): LanguageConfigDto {
        val tenant = loadTenant()
        val supported = tenant.supportedLangs.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return LanguageConfigDto(
            defaultLang = tenant.defaultLang,
            supportedLangs = supported.map { code ->
                LanguageDto(code = code, name = LANGUAGE_NAMES[code] ?: code)
            }
        )
    }

    @PutMapping
    @Transactional
    fun updateLanguages(@RequestBody request: UpdateLanguagesRequest): LanguageConfigDto {
        val tenant = loadTenant()

        val langs = request.supportedLangs
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()

        // Ensure default lang is always in the list
        val defaultLang = request.defaultLang?.trim()?.lowercase() ?: tenant.defaultLang
        val allLangs = (listOf(defaultLang) + langs).distinct()

        tenant.defaultLang = defaultLang
        tenant.supportedLangs = allLangs.joinToString(",")
        tenant.updatedAt = Instant.now()
        tenantRepository.save(tenant)

        return LanguageConfigDto(
            defaultLang = tenant.defaultLang,
            supportedLangs = allLangs.map { code ->
                LanguageDto(code = code, name = LANGUAGE_NAMES[code] ?: code)
            }
        )
    }

    private fun loadTenant(): com.hps.domain.tenant.Tenant {
        val tenantId = TenantContext.require()
        return tenantRepository.findById(tenantId)
            .orElseThrow { NotFoundException("Tenant", tenantId) }
    }

    companion object {
        val LANGUAGE_NAMES = mapOf(
            "en" to "English",
            "pl" to "Polish",
            "uk" to "Ukrainian",
            "de" to "German",
            "fr" to "French",
            "es" to "Spanish",
            "it" to "Italian",
            "pt" to "Portuguese",
            "nl" to "Dutch",
            "cs" to "Czech",
            "sk" to "Slovak",
            "ro" to "Romanian",
            "hu" to "Hungarian",
            "bg" to "Bulgarian",
            "hr" to "Croatian",
            "sr" to "Serbian",
            "sl" to "Slovenian",
            "lt" to "Lithuanian",
            "lv" to "Latvian",
            "et" to "Estonian",
            "fi" to "Finnish",
            "sv" to "Swedish",
            "da" to "Danish",
            "no" to "Norwegian",
            "el" to "Greek",
            "tr" to "Turkish",
            "ar" to "Arabic",
            "he" to "Hebrew",
            "zh" to "Chinese",
            "ja" to "Japanese",
            "ko" to "Korean",
            "th" to "Thai",
            "vi" to "Vietnamese",
            "ru" to "Russian",
            "hi" to "Hindi"
        )
    }
}

data class LanguageDto(
    val code: String,
    val name: String
)

data class LanguageConfigDto(
    val defaultLang: String,
    val supportedLangs: List<LanguageDto>
)

data class UpdateLanguagesRequest(
    val defaultLang: String? = null,
    val supportedLangs: List<String> = emptyList()
)
