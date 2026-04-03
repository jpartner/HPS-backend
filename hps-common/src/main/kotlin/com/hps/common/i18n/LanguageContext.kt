package com.hps.common.i18n

object LanguageContext {
    private val currentLanguage = ThreadLocal.withInitial { Language.EN }

    fun get(): Language = currentLanguage.get()

    fun set(language: Language) {
        currentLanguage.set(language)
    }

    fun clear() {
        currentLanguage.remove()
    }
}

enum class Language(val code: String) {
    EN("en"),
    PL("pl"),
    UK("uk");

    companion object {
        fun fromCode(code: String?): Language =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}
