package com.hps.api.pricing

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/pricing")
class PricingController(
    private val pricingService: PricingService
) {

    @PostMapping("/calculate")
    fun calculate(
        @RequestBody request: PricingCalculateRequest,
        @RequestHeader("Accept-Language", defaultValue = "en") lang: String
    ): PricingBreakdownDto {
        return pricingService.calculate(request, lang.take(2))
    }
}
