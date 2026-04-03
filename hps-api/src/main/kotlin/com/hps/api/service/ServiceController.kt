package com.hps.api.service

import com.hps.common.i18n.LanguageContext
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class ServiceController(
    private val serviceManagementService: ServiceManagementService
) {
    @GetMapping("/services/{id}")
    fun getService(@PathVariable id: UUID): ServiceDto {
        val lang = LanguageContext.get().code
        return serviceManagementService.getById(id, lang)
    }

    @GetMapping("/providers/{providerId}/services")
    fun listProviderServices(@PathVariable providerId: UUID): List<ServiceDto> {
        val lang = LanguageContext.get().code
        return serviceManagementService.listByProvider(providerId, lang)
    }

    @PostMapping("/providers/me/services")
    @ResponseStatus(HttpStatus.CREATED)
    fun createService(
        @Valid @RequestBody request: CreateServiceRequest,
        auth: Authentication
    ): ServiceDto {
        val userId = UUID.fromString(auth.principal as String)
        return serviceManagementService.create(userId, request)
    }

    @PutMapping("/providers/me/services/{serviceId}")
    fun updateService(
        @PathVariable serviceId: UUID,
        @Valid @RequestBody request: UpdateServiceRequest,
        auth: Authentication
    ): ServiceDto {
        val userId = UUID.fromString(auth.principal as String)
        return serviceManagementService.update(userId, serviceId, request)
    }

    @DeleteMapping("/providers/me/services/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteService(
        @PathVariable serviceId: UUID,
        auth: Authentication
    ) {
        val userId = UUID.fromString(auth.principal as String)
        serviceManagementService.delete(userId, serviceId)
    }
}
