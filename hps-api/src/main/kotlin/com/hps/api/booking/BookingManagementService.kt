package com.hps.api.booking

import com.hps.common.exception.BadRequestException
import com.hps.common.exception.ForbiddenException
import com.hps.common.exception.NotFoundException
import com.hps.common.i18n.bestTranslation
import com.hps.domain.booking.*
import com.hps.domain.user.UserRole
import com.hps.persistence.booking.BookingRepository
import com.hps.persistence.service.ServiceRepository
import com.hps.persistence.user.ProviderProfileRepository
import com.hps.persistence.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BookingManagementService(
    private val bookingRepository: BookingRepository,
    private val serviceRepository: ServiceRepository,
    private val providerRepository: ProviderProfileRepository,
    private val userRepository: UserRepository
) {
    fun calculateFee(providerId: UUID, services: List<BookingServiceRequest>, lang: String): BookingCalculation {
        val lineItems = services.map { req ->
            val service = serviceRepository.findByIdWithTranslations(req.serviceId, lang)
                ?: throw NotFoundException("Service", req.serviceId)
            if (service.provider.userId != providerId) {
                throw BadRequestException("Service ${req.serviceId} does not belong to this provider")
            }
            val lineTotal = service.priceAmount.multiply(BigDecimal(req.quantity))
            val lineDuration = (service.durationMinutes ?: 0) * req.quantity
            BookingServiceDto(
                serviceId = service.id,
                serviceTitle = service.translations.bestTranslation(lang, { it.lang }, { it.title }),
                quantity = req.quantity,
                unitPrice = service.priceAmount,
                lineTotal = lineTotal,
                durationMinutes = if (lineDuration > 0) lineDuration else null
            ) to service.priceCurrency
        }

        val currencies = lineItems.map { it.second }.distinct()
        if (currencies.size > 1) throw BadRequestException("All services must use the same currency")

        val total = lineItems.sumOf { it.first.lineTotal }
        val totalDuration = lineItems.sumOf { it.first.durationMinutes ?: 0 }

        return BookingCalculation(
            services = lineItems.map { it.first },
            totalAmount = total,
            totalDurationMinutes = totalDuration,
            currency = currencies.first()
        )
    }

    @Transactional
    fun createBooking(clientId: UUID, request: CreateBookingRequest, lang: String): BookingDto {
        val client = userRepository.findById(clientId)
            .orElseThrow { NotFoundException("User", clientId) }
        val provider = providerRepository.findById(request.providerId)
            .orElseThrow { NotFoundException("Provider", request.providerId) }

        val calculation = calculateFee(request.providerId, request.services, lang)

        val booking = Booking(
            client = client,
            provider = provider,
            bookingType = BookingType.valueOf(request.bookingType),
            scheduledAt = request.scheduledAt,
            totalDurationMinutes = calculation.totalDurationMinutes,
            durationMinutes = calculation.totalDurationMinutes,
            priceAmount = calculation.totalAmount,
            originalAmount = calculation.totalAmount,
            priceCurrency = calculation.currency,
            locationLat = request.locationLat,
            locationLng = request.locationLng,
            addressText = request.addressText,
            clientNotes = request.clientNotes
        )

        calculation.services.forEach { item ->
            val service = serviceRepository.findById(item.serviceId).get()
            booking.bookingServices.add(
                BookingService(
                    booking = booking,
                    service = service,
                    serviceTitle = item.serviceTitle,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    lineTotal = item.lineTotal,
                    durationMinutes = item.durationMinutes
                )
            )
        }

        booking.statusHistory.add(
            BookingStatusHistory(
                booking = booking,
                newStatus = BookingStatus.REQUESTED,
                changedBy = client
            )
        )

        bookingRepository.save(booking)
        return booking.toDto()
    }

    @Transactional
    fun quoteBooking(providerId: UUID, bookingId: UUID, request: QuoteBookingRequest): BookingDto {
        val booking = getBookingForProvider(providerId, bookingId)

        if (booking.status != BookingStatus.REQUESTED) {
            throw BadRequestException("Can only quote bookings in REQUESTED status")
        }

        val provider = userRepository.findById(providerId)
            .orElseThrow { NotFoundException("User", providerId) }

        booking.priceAmount = request.priceAmount
        request.providerNotes?.let { booking.providerNotes = it }
        booking.status = BookingStatus.QUOTED
        booking.updatedAt = Instant.now()

        booking.statusHistory.add(
            BookingStatusHistory(
                booking = booking,
                oldStatus = BookingStatus.REQUESTED,
                newStatus = BookingStatus.QUOTED,
                changedBy = provider,
                reason = "Price updated to ${request.priceAmount}"
            )
        )

        bookingRepository.save(booking)
        return booking.toDto()
    }

    @Transactional
    fun updateStatus(userId: UUID, userRole: UserRole, bookingId: UUID, request: UpdateStatusRequest): BookingDto {
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { NotFoundException("Booking", bookingId) }

        val newStatus = BookingStatus.valueOf(request.status)
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User", userId) }

        validateStatusTransition(booking, newStatus, userId, userRole)

        val oldStatus = booking.status
        booking.status = newStatus
        booking.updatedAt = Instant.now()

        booking.statusHistory.add(
            BookingStatusHistory(
                booking = booking,
                oldStatus = oldStatus,
                newStatus = newStatus,
                changedBy = user,
                reason = request.reason
            )
        )

        bookingRepository.save(booking)
        return booking.toDto()
    }

    fun getBooking(userId: UUID, userRole: UserRole, bookingId: UUID): BookingDto {
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { NotFoundException("Booking", bookingId) }

        if (userRole != UserRole.ADMIN &&
            booking.client.id != userId &&
            booking.provider.userId != userId) {
            throw ForbiddenException("You do not have access to this booking")
        }

        return booking.toDto()
    }

    fun listBookings(userId: UUID, userRole: UserRole): List<BookingDto> {
        val bookings = when (userRole) {
            UserRole.CLIENT -> bookingRepository.findByClientId(userId)
            UserRole.PROVIDER -> bookingRepository.findByProviderId(userId)
            UserRole.ADMIN -> bookingRepository.findAll()
        }
        return bookings.map { it.toDto() }
    }

    private fun validateStatusTransition(
        booking: Booking, newStatus: BookingStatus,
        userId: UUID, userRole: UserRole
    ) {
        val isClient = booking.client.id == userId
        val isProvider = booking.provider.userId == userId
        val isAdmin = userRole == UserRole.ADMIN

        if (!isClient && !isProvider && !isAdmin) {
            throw ForbiddenException("You are not a party to this booking")
        }

        val allowed = when (booking.status) {
            BookingStatus.REQUESTED -> when {
                // Provider confirms at original price or cancels
                (isProvider || isAdmin) && newStatus == BookingStatus.CONFIRMED -> true
                (isProvider || isAdmin) && newStatus == BookingStatus.CANCELLED_BY_PROVIDER -> true
                // Client cancels
                (isClient || isAdmin) && newStatus == BookingStatus.CANCELLED_BY_CLIENT -> true
                else -> false
            }
            BookingStatus.QUOTED -> when {
                // Client accepts the quoted price → CONFIRMED
                (isClient || isAdmin) && newStatus == BookingStatus.CONFIRMED -> true
                // Client rejects / cancels
                (isClient || isAdmin) && newStatus == BookingStatus.CANCELLED_BY_CLIENT -> true
                // Provider cancels
                (isProvider || isAdmin) && newStatus == BookingStatus.CANCELLED_BY_PROVIDER -> true
                else -> false
            }
            BookingStatus.CONFIRMED -> when {
                (isProvider || isAdmin) && newStatus == BookingStatus.IN_PROGRESS -> true
                (isProvider || isAdmin) && newStatus == BookingStatus.CANCELLED_BY_PROVIDER -> true
                (isClient || isAdmin) && newStatus == BookingStatus.CANCELLED_BY_CLIENT -> true
                (isProvider || isAdmin) && newStatus == BookingStatus.NO_SHOW -> true
                else -> false
            }
            BookingStatus.IN_PROGRESS -> when {
                (isProvider || isAdmin) && newStatus == BookingStatus.COMPLETED -> true
                else -> false
            }
            else -> false // Terminal states
        }

        if (!allowed) {
            throw BadRequestException(
                "Cannot transition from ${booking.status} to $newStatus" +
                if (!isAdmin) " with your role" else ""
            )
        }
    }

    private fun getBookingForProvider(providerId: UUID, bookingId: UUID): Booking {
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { NotFoundException("Booking", bookingId) }
        if (booking.provider.userId != providerId) {
            throw ForbiddenException("This booking does not belong to you")
        }
        return booking
    }

    private fun Booking.toDto() = BookingDto(
        id = id,
        clientId = client.id,
        clientName = client.profile?.firstName,
        providerId = provider.userId!!,
        providerName = provider.businessName,
        status = status.name,
        bookingType = bookingType.name,
        scheduledAt = scheduledAt,
        totalDurationMinutes = totalDurationMinutes,
        priceAmount = priceAmount,
        originalAmount = originalAmount,
        priceCurrency = priceCurrency,
        locationLat = locationLat,
        locationLng = locationLng,
        addressText = addressText,
        clientNotes = clientNotes,
        providerNotes = providerNotes,
        services = bookingServices.map {
            BookingServiceDto(
                serviceId = it.service.id,
                serviceTitle = it.serviceTitle,
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                lineTotal = it.lineTotal,
                durationMinutes = it.durationMinutes
            )
        },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
