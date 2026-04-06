package com.hps.api.provider

import com.hps.api.auth.userId
import com.hps.common.exception.BadRequestException
import com.hps.common.exception.ForbiddenException
import com.hps.common.exception.NotFoundException
import com.hps.common.storage.FileStorageService
import com.hps.domain.user.MediaApprovalStatus
import com.hps.domain.user.MediaType
import com.hps.domain.user.ProviderMedia
import com.hps.persistence.user.ProviderMediaRepository
import com.hps.persistence.user.ProviderProfileRepository
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class ProviderMediaController(
    private val mediaRepository: ProviderMediaRepository,
    private val providerRepository: ProviderProfileRepository,
    private val storageService: FileStorageService
) {
    companion object {
        private const val MAX_GALLERY = 20
        private const val MAX_VERIFICATION = 10
        private const val MAX_AVATAR = 1
        private val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
        private val ALLOWED_VIDEO_TYPES = setOf("video/mp4", "video/quicktime", "video/webm")
        private val ALLOWED_TYPES = ALLOWED_IMAGE_TYPES + ALLOWED_VIDEO_TYPES
    }

    // === Public endpoints ===

    /** Public gallery: only APPROVED, GALLERY, not private */
    @GetMapping("/providers/{providerId}/gallery")
    fun getPublicGallery(@PathVariable providerId: UUID): List<GalleryImageDto> {
        return mediaRepository.findByProviderUserIdAndMediaTypeAndApprovalStatusAndIsPrivateOrderBySortOrder(
            providerId, MediaType.GALLERY, MediaApprovalStatus.APPROVED, false
        ).map { it.toGalleryDto() }
    }

    /** Public media endpoint (same as gallery for now) */
    @GetMapping("/providers/{providerId}/media")
    fun getPublicMedia(@PathVariable providerId: UUID): List<GalleryImageDto> = getPublicGallery(providerId)

    // === Provider's own media management ===

    /** List all own media (all statuses, all types) */
    @GetMapping("/providers/me/media")
    fun getOwnMedia(
        @RequestParam(required = false) mediaType: String?,
        auth: Authentication
    ): List<MediaDto> {
        val providerId = auth.userId()
        val items = if (mediaType != null) {
            val type = parseMediaType(mediaType)
            mediaRepository.findByProviderUserIdAndMediaTypeOrderBySortOrder(providerId, type)
        } else {
            mediaRepository.findByProviderUserIdOrderBySortOrder(providerId)
        }
        return items.map { it.toMediaDto() }
    }

    /** Upload media file */
    @PostMapping("/providers/me/media", consumes = [org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun uploadMedia(
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false) caption: String?,
        @RequestParam(required = false, defaultValue = "GALLERY") mediaType: String,
        @RequestParam(required = false, defaultValue = "false") isPrivate: Boolean,
        @RequestParam(required = false, defaultValue = "false") blurRequested: Boolean,
        auth: Authentication
    ): MediaDto {
        val providerId = auth.userId()
        val provider = providerRepository.findById(providerId)
            .orElseThrow { NotFoundException("Provider", providerId) }

        if (!ALLOWED_TYPES.contains(file.contentType)) {
            throw BadRequestException("File type not allowed. Use JPEG, PNG, WebP, GIF, MP4, MOV, or WebM")
        }

        val type = parseMediaType(mediaType)
        enforceLimit(providerId, type)

        val folder = "provider-media/$providerId/${type.name.lowercase()}"
        val key = storageService.store(
            folder,
            file.originalFilename ?: "upload",
            file.contentType ?: "application/octet-stream",
            file.inputStream
        )

        val count = mediaRepository.countByProviderUserIdAndMediaType(providerId, type)
        val media = ProviderMedia(
            provider = provider,
            url = storageService.resolveUrl(key),
            storageKey = key,
            caption = caption,
            sortOrder = count,
            mediaType = type,
            contentType = file.contentType,
            approvalStatus = MediaApprovalStatus.PENDING,
            isPrivate = if (type == MediaType.VERIFICATION) true else isPrivate,
            blurRequested = blurRequested
        )

        mediaRepository.save(media)
        return media.toMediaDto()
    }

    /** Add media by URL */
    @PostMapping("/providers/me/media/url")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun addMediaByUrl(
        @RequestBody request: GalleryAddUrlRequest,
        auth: Authentication
    ): MediaDto {
        val providerId = auth.userId()
        val provider = providerRepository.findById(providerId)
            .orElseThrow { NotFoundException("Provider", providerId) }

        val type = parseMediaType(request.mediaType)
        enforceLimit(providerId, type)

        val count = mediaRepository.countByProviderUserIdAndMediaType(providerId, type)
        val media = ProviderMedia(
            provider = provider,
            url = request.url,
            storageKey = null,
            caption = request.caption,
            sortOrder = count,
            mediaType = type,
            approvalStatus = MediaApprovalStatus.PENDING,
            isPrivate = if (type == MediaType.VERIFICATION) true else request.isPrivate,
            blurRequested = request.blurRequested
        )

        mediaRepository.save(media)
        return media.toMediaDto()
    }

    /** Update media metadata */
    @PutMapping("/providers/me/media/{id}")
    @Transactional
    fun updateMedia(
        @PathVariable id: UUID,
        @RequestBody request: MediaUpdateRequest,
        auth: Authentication
    ): MediaDto {
        val media = getOwnMedia(auth.userId(), id)
        request.caption?.let { media.caption = it }
        request.blurRequested?.let { media.blurRequested = it }
        request.isPrivate?.let {
            // Verification media must stay private
            if (media.mediaType != MediaType.VERIFICATION) media.isPrivate = it
        }
        mediaRepository.save(media)
        return media.toMediaDto()
    }

    /** Reorder media within a type */
    @PutMapping("/providers/me/media/order")
    @Transactional
    fun reorderMedia(
        @RequestBody request: GalleryReorderRequest,
        auth: Authentication
    ): List<MediaDto> {
        val providerId = auth.userId()
        val all = mediaRepository.findByProviderUserIdOrderBySortOrder(providerId)
        val map = all.associateBy { it.id }

        request.imageIds.forEachIndexed { index, id ->
            val item = map[id] ?: throw NotFoundException("Media", id)
            item.sortOrder = index
        }

        mediaRepository.saveAll(all)
        return all.sortedBy { it.sortOrder }.map { it.toMediaDto() }
    }

    /** Delete media */
    @DeleteMapping("/providers/me/media/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun deleteMedia(@PathVariable id: UUID, auth: Authentication) {
        val media = getOwnMedia(auth.userId(), id)
        media.storageKey?.let { storageService.delete(it) }
        mediaRepository.delete(media)
    }

    // === Private helpers ===

    private fun getOwnMedia(providerId: UUID, mediaId: UUID): ProviderMedia {
        val media = mediaRepository.findById(mediaId)
            .orElseThrow { NotFoundException("Media", mediaId) }
        if (media.provider.userId != providerId) throw ForbiddenException("Not your media")
        return media
    }

    private fun parseMediaType(value: String): MediaType = try {
        MediaType.valueOf(value.uppercase())
    } catch (e: IllegalArgumentException) {
        throw BadRequestException("Invalid media type: $value")
    }

    private fun enforceLimit(providerId: UUID, type: MediaType) {
        val count = mediaRepository.countByProviderUserIdAndMediaType(providerId, type)
        val max = when (type) {
            MediaType.GALLERY -> MAX_GALLERY
            MediaType.VERIFICATION -> MAX_VERIFICATION
            MediaType.AVATAR -> MAX_AVATAR
        }
        if (count >= max) {
            throw BadRequestException("${type.name} limit reached (max $max)")
        }
    }

    private fun ProviderMedia.toGalleryDto() = GalleryImageDto(
        id = id, url = url, caption = caption, sortOrder = sortOrder
    )

    private fun ProviderMedia.toMediaDto() = MediaDto(
        id = id, url = url, caption = caption, sortOrder = sortOrder,
        mediaType = mediaType.name, contentType = contentType,
        isVideo = isVideo, approvalStatus = approvalStatus.name,
        reviewNote = reviewNote, isPrivate = isPrivate,
        blurRequested = blurRequested, createdAt = createdAt
    )
}
