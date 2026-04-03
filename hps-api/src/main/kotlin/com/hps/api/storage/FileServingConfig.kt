package com.hps.api.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class FileServingConfig(
    @Value("\${hps.storage.local.base-dir:./uploads}") private val baseDir: String
) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/files/**")
            .addResourceLocations("file:$baseDir/")
    }
}
