package com.hps.api.config

import com.hps.common.i18n.Language
import com.hps.common.i18n.LanguageContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class LanguageInterceptor : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val acceptLanguage = request.getHeader("Accept-Language")
        val langCode = acceptLanguage?.split(",")?.firstOrNull()?.split("-")?.firstOrNull()?.trim()
        LanguageContext.set(Language.fromCode(langCode))
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        LanguageContext.clear()
    }
}
