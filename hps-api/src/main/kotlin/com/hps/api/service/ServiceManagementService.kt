package com.hps.api.service

import com.hps.common.exception.ForbiddenException
import com.hps.common.exception.NotFoundException
import com.hps.common.i18n.bestTranslation
import com.hps.domain.service.PricingType
import com.hps.domain.service.Service
import com.hps.domain.service.ServiceTranslation
import com.hps.persistence.service.ServiceCategoryRepository
import com.hps.persistence.service.ServiceRepository
import com.hps.persistence.service.ServiceTemplateRepository
import com.hps.persistence.user.ProviderProfileRepository
import com.hps.common.tenant.TenantContext
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
class ServiceManagementService(
    private val serviceRepository: ServiceRepository,
    private val providerRepository: ProviderProfileRepository,
    private val templateRepository: ServiceTemplateRepository,
    private val categoryRepository: ServiceCategoryRepository
) {
    fun listByProvider(providerId: UUID, lang: String): List<ServiceDto> {
        return serviceRepository.findByProviderWithTranslations(providerId, lang)
            .map { it.toDto(lang) }
    }

    fun getById(serviceId: UUID, lang: String): ServiceDto {
        val service = serviceRepository.findByIdWithTranslations(serviceId, lang)
            ?: throw NotFoundException("Service", serviceId)
        return service.toDto(lang)
    }

    @Transactional
    fun create(providerId: UUID, request: CreateServiceRequest): ServiceDto {
        val provider = providerRepository.findById(providerId)
            .orElseThrow { NotFoundException("Provider", providerId) }

        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { NotFoundException("Category", request.categoryId) }

        val template = request.templateId?.let {
            templateRepository.findById(it).orElse(null)
        }

        val tenantId = TenantContext.require()
        val service = Service(
            tenantId = tenantId,
            provider = provider,
            category = category,
            template = template,
            pricingType = PricingType.valueOf(request.pricingType),
            priceAmount = request.priceAmount,
            priceCurrency = request.priceCurrency,
            durationMinutes = request.durationMinutes ?: template?.defaultDurationMinutes,
            isIncluded = request.isIncluded,
            primaryAmount = request.primaryAmount ?: request.priceAmount,
            secondaryAmount = request.secondaryAmount,
            minDurationMinutes = request.minDurationMinutes
        )

        request.translations.forEach { t ->
            service.translations.add(
                ServiceTranslation(service = service, lang = t.lang, title = t.title, description = t.description)
            )
        }

        serviceRepository.save(service)
        return service.toDto("en")
    }

    @Transactional
    fun update(providerId: UUID, serviceId: UUID, request: UpdateServiceRequest): ServiceDto {
        val service = serviceRepository.findById(serviceId)
            .orElseThrow { NotFoundException("Service", serviceId) }

        if (service.provider.userId != providerId) {
            throw ForbiddenException("Not your service")
        }

        request.categoryId?.let {
            service.category = categoryRepository.findById(it)
                .orElseThrow { NotFoundException("Category", it) }
        }
        request.pricingType?.let { service.pricingType = PricingType.valueOf(it) }
        request.priceAmount?.let { service.priceAmount = it }
        request.priceCurrency?.let { service.priceCurrency = it }
        request.durationMinutes?.let { service.durationMinutes = it }
        request.isIncluded?.let { service.isIncluded = it }
        request.primaryAmount?.let { service.primaryAmount = it }
        request.secondaryAmount?.let { service.secondaryAmount = it }
        request.minDurationMinutes?.let { service.minDurationMinutes = it }
        request.isActive?.let { service.isActive = it }

        if (request.translations != null) {
            service.translations.clear()
            serviceRepository.saveAndFlush(service)
            request.translations.forEach { t ->
                service.translations.add(
                    ServiceTranslation(service = service, lang = t.lang, title = t.title, description = t.description)
                )
            }
        }

        service.updatedAt = Instant.now()
        serviceRepository.save(service)
        return service.toDto("en")
    }

    @Transactional
    fun delete(providerId: UUID, serviceId: UUID) {
        val service = serviceRepository.findById(serviceId)
            .orElseThrow { NotFoundException("Service", serviceId) }

        if (service.provider.userId != providerId) {
            throw ForbiddenException("Not your service")
        }

        serviceRepository.delete(service)
    }

    private fun Service.toDto(lang: String) = ServiceDto(
        id = id,
        title = translations.bestTranslation(lang, { it.lang }, { it.title }),
        description = translations.firstOrNull { it.lang == lang }?.description
            ?: translations.firstOrNull { it.lang == "en" }?.description,
        categoryId = category.id,
        categoryName = category.translations.bestTranslation(lang, { it.lang }, { it.name }),
        pricingType = pricingType.name,
        priceAmount = priceAmount,
        priceCurrency = priceCurrency,
        durationMinutes = durationMinutes,
        isIncluded = isIncluded,
        primaryAmount = primaryAmount,
        secondaryAmount = secondaryAmount,
        minDurationMinutes = minDurationMinutes,
        isActive = isActive,
        providerId = provider.userId!!,
        providerName = provider.businessName
    )
}
