package com.hps.api.admin

import com.hps.api.auth.userId
import com.hps.api.provider.MediaDto
import com.hps.common.exception.BadRequestException
import com.hps.common.exception.NotFoundException
import com.hps.common.tenant.TenantContext
import com.hps.domain.user.MediaApprovalStatus
import com.hps.domain.user.ProviderMedia
import com.hps.persistence.user.ProviderMediaRepository
import com.hps.persistence.user.UserRepository
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

data class AdminMediaDto(
    val id: UUID,
    val providerId: UUID,
    val providerHandle: String?,
    val providerEmail: String,
    val url: String,
    val thumbnailUrl: String?,
    val caption: String?,
    val mediaType: String,
    val contentType: String?,
    val isVideo: Boolean,
    val approvalStatus: String,
    val reviewNote: String?,
    val isPrivate: Boolean,
    val blurRequested: Boolean,
    val cdnStatus: String,
    val fileSizeBytes: Long?,
    val createdAt: Instant
)

data class ReviewMediaRequest(
    val approvalStatus: String,
    val note: String? = null
)

@RestController
@RequestMapping("/api/v1/admin/media")
class AdminMediaController(
    private val mediaRepository: ProviderMediaRepository,
    private val userRepository: UserRepository
) {

    @GetMapping
    @Transactional(readOnly = true)
    fun listMedia(
        @RequestParam(required = false, defaultValue = "PENDING") status: String
    ): List<AdminMediaDto> {
        val tenantId = TenantContext.require()
        val approvalStatus = try {
            MediaApprovalStatus.valueOf(status.uppercase())
        } catch (e: IllegalArgumentException) {
            throw BadRequestException("Invalid status: $status")
        }
        return mediaRepository.findByTenantIdAndApprovalStatus(tenantId, approvalStatus)
            .map { it.toAdminDto() }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    fun getMedia(@PathVariable id: UUID): AdminMediaDto {
        val media = mediaRepository.findById(id)
            .orElseThrow { NotFoundException("Media", id) }
        val tenantId = TenantContext.require()
        if (media.provider.tenantId != tenantId) throw NotFoundException("Media", id)
        return media.toAdminDto()
    }

    @PutMapping("/{id}/review")
    @Transactional
    fun reviewMedia(
        @PathVariable id: UUID,
        @RequestBody request: ReviewMediaRequest,
        auth: Authentication
    ): AdminMediaDto {
        val media = mediaRepository.findById(id)
            .orElseThrow { NotFoundException("Media", id) }
        val tenantId = TenantContext.require()
        if (media.provider.tenantId != tenantId) throw NotFoundException("Media", id)

        val status = try {
            MediaApprovalStatus.valueOf(request.approvalStatus.uppercase())
        } catch (e: IllegalArgumentException) {
            throw BadRequestException("Invalid approval status: ${request.approvalStatus}")
        }

        val reviewer = userRepository.findById(auth.userId())
            .orElseThrow { NotFoundException("User", auth.userId()) }

        media.approvalStatus = status
        media.reviewNote = request.note
        media.reviewedAt = Instant.now()
        media.reviewedBy = reviewer
        mediaRepository.save(media)

        return media.toAdminDto()
    }

    private fun ProviderMedia.toAdminDto() = AdminMediaDto(
        id = id,
        providerId = provider.userId!!,
        providerHandle = provider.user.handle,
        providerEmail = provider.user.email,
        url = url,
        thumbnailUrl = publicThumbnailUrl,
        caption = caption,
        mediaType = mediaType.name,
        contentType = contentType,
        isVideo = isVideo,
        approvalStatus = approvalStatus.name,
        reviewNote = reviewNote,
        isPrivate = isPrivate,
        blurRequested = blurRequested,
        cdnStatus = cdnStatus.name,
        fileSizeBytes = fileSizeBytes,
        createdAt = createdAt
    )
}
