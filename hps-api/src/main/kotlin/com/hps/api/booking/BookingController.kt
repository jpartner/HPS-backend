package com.hps.api.booking

import com.hps.api.auth.role
import com.hps.api.auth.userId
import com.hps.common.i18n.LanguageContext
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/bookings")
class BookingController(
    private val bookingService: BookingManagementService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createBooking(
        @Valid @RequestBody request: CreateBookingRequest,
        auth: Authentication
    ): BookingDto {
        val lang = LanguageContext.get().code
        return bookingService.createBooking(auth.userId(), request, lang)
    }

    @PostMapping("/calculate")
    fun calculateFee(
        @RequestParam providerId: UUID,
        @Valid @RequestBody services: List<BookingServiceRequest>
    ): BookingCalculation {
        val lang = LanguageContext.get().code
        return bookingService.calculateFee(providerId, services, lang)
    }

    @GetMapping
    fun listBookings(auth: Authentication): List<BookingDto> =
        bookingService.listBookings(auth.userId(), auth.role())

    @GetMapping("/{id}")
    fun getBooking(@PathVariable id: UUID, auth: Authentication): BookingDto =
        bookingService.getBooking(auth.userId(), auth.role(), id)

    @PostMapping("/{id}/quote")
    fun quoteBooking(
        @PathVariable id: UUID,
        @Valid @RequestBody request: QuoteBookingRequest,
        auth: Authentication
    ): BookingDto =
        bookingService.quoteBooking(auth.userId(), id, request)

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateStatusRequest,
        auth: Authentication
    ): BookingDto =
        bookingService.updateStatus(auth.userId(), auth.role(), id, request)
}
