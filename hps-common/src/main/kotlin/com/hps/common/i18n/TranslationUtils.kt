package com.hps.common.i18n

fun <T> Collection<T>.bestTranslation(lang: String, langExtractor: (T) -> String, nameExtractor: (T) -> String): String {
    return firstOrNull { langExtractor(it) == lang }?.let(nameExtractor)
        ?: firstOrNull { langExtractor(it) == "en" }?.let(nameExtractor)
        ?: firstOrNull()?.let(nameExtractor)
        ?: ""
}
