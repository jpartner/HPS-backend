package com.hps.app.seed

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hps.api.admin.TenantConfigExport
import com.hps.api.admin.TenantConfigService
import com.hps.persistence.tenant.TenantRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * Seeds the default tenant on startup if no tenants exist.
 *
 * Reads config from classpath:seed/default-tenant.json.
 * Runs after SuperAdminSeeder (Order 1).
 */
@Component
@Order(10)
class TenantConfigSeeder(
    private val tenantRepository: TenantRepository,
    private val tenantConfigService: TenantConfigService
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(TenantConfigSeeder::class.java)
    private val mapper = jacksonObjectMapper()

    override fun run(args: ApplicationArguments) {
        if (tenantRepository.count() > 0) {
            log.info("Tenants already exist, skipping default tenant seed")
            return
        }

        val resource = ClassPathResource("seed/default-tenant.json")
        if (!resource.exists()) {
            log.warn("No seed/default-tenant.json found on classpath, skipping tenant seed")
            return
        }

        val config: TenantConfigExport = resource.inputStream.use { mapper.readValue(it) }
        val result = tenantConfigService.importConfig(config)

        log.info(
            "Seeded tenant '{}' ({}): {} categories, {} templates, {} attributes, admin={}",
            result.slug, result.tenantId,
            result.categoriesCreated, result.templatesCreated,
            result.attributesCreated, result.adminCreated
        )
    }
}
