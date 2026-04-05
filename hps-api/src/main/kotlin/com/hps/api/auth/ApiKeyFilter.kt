package com.hps.api.auth

import com.hps.common.tenant.TenantContext
import com.hps.persistence.tenant.ApiKeyRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class ApiKeyFilter(
    private val apiKeyRepository: ApiKeyRepository
) : OncePerRequestFilter() {

    private val passwordEncoder = BCryptPasswordEncoder()

    private val log = LoggerFactory.getLogger(ApiKeyFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val clientIdHeader = request.getHeader("X-Client-Id")
            val clientSecretHeader = request.getHeader("X-Client-Secret")
            val tenantIdHeader = request.getHeader("X-Tenant-Id")

            if (clientIdHeader != null && clientSecretHeader != null) {
                // API key authentication — validate credentials
                try {
                    val clientId = UUID.fromString(clientIdHeader)
                    val apiKey = apiKeyRepository.findActiveByClientId(clientId)

                    if (apiKey != null && passwordEncoder.matches(clientSecretHeader, apiKey.clientSecretHash)) {
                        TenantContext.set(apiKey.tenant.id)
                    } else {
                        log.warn("Invalid API key: clientId=$clientIdHeader")
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API credentials")
                        return
                    }
                } catch (e: IllegalArgumentException) {
                    log.warn("Malformed X-Client-Id: $clientIdHeader")
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-Client-Id format")
                    return
                }
            } else if (tenantIdHeader != null) {
                // Admin tenant selection — JWT auth will be validated by Spring Security
                try {
                    TenantContext.set(UUID.fromString(tenantIdHeader))
                } catch (e: IllegalArgumentException) {
                    log.warn("Malformed X-Tenant-Id: $tenantIdHeader")
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-Tenant-Id format")
                    return
                }
            }

            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }
}
