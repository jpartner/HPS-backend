package com.hps.api.provider

import com.hps.api.auth.userId
import com.hps.common.exception.BadRequestException
import com.hps.common.exception.ForbiddenException
import com.hps.common.exception.NotFoundException
import com.hps.common.storage.FileStorageService
import com.hps.domain.user.ProviderGalleryImage
import com.hps.persistence.user.ProviderGalleryImageRepository
import com.hps.persistence.user.ProviderProfileRepository
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class ProviderGalleryController(
    private val galleryRepository: ProviderGalleryImageRepository,
    private val providerRepository: ProviderProfileRepository,
    private val storageService: FileStorageService
) {
    companion object {
        private const val MAX_GALLERY_SIZE = 20
        private val ALLOWED_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
    }

    @GetMapping("/providers/{providerId}/gallery")
    fun getGallery(@PathVariable providerId: UUID): List<GalleryImageDto> {
        return galleryRepository.findByProviderUserIdOrderBySortOrder(providerId)
            .map { it.toDto() }
    }

    @PostMapping("/providers/me/gallery", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun uploadImage(
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false) caption: String?,
        auth: Authentication
    ): GalleryImageDto {
        val providerId = auth.userId()
        val provider = providerRepository.findById(providerId)
            .orElseThrow { NotFoundException("Provider", providerId) }

        if (!ALLOWED_TYPES.contains(file.contentType)) {
            throw BadRequestException("File type not allowed. Use JPEG, PNG, WebP, or GIF")
        }

        val count = galleryRepository.countByProviderUserId(providerId)
        if (count >= MAX_GALLERY_SIZE) {
            throw BadRequestException("Gallery is full (max $MAX_GALLERY_SIZE images)")
        }

        val key = storageService.store(
            "provider-gallery/$providerId",
            file.originalFilename ?: "image.jpg",
            file.contentType ?: "image/jpeg",
            file.inputStream
        )

        val image = ProviderGalleryImage(
            provider = provider,
            url = storageService.resolveUrl(key),
            storageKey = key,
            caption = caption,
            sortOrder = count
        )

        galleryRepository.save(image)
        return image.toDto()
    }

    @PostMapping("/providers/me/gallery/url")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun addImageByUrl(
        @RequestBody request: GalleryAddUrlRequest,
        auth: Authentication
    ): GalleryImageDto {
        val providerId = auth.userId()
        val provider = providerRepository.findById(providerId)
            .orElseThrow { NotFoundException("Provider", providerId) }

        val count = galleryRepository.countByProviderUserId(providerId)
        if (count >= MAX_GALLERY_SIZE) {
            throw BadRequestException("Gallery is full (max $MAX_GALLERY_SIZE images)")
        }

        val image = ProviderGalleryImage(
            provider = provider,
            url = request.url,
            storageKey = null,
            caption = request.caption,
            sortOrder = count
        )

        galleryRepository.save(image)
        return image.toDto()
    }

    @PutMapping("/providers/me/gallery/{imageId}")
    @Transactional
    fun updateImage(
        @PathVariable imageId: UUID,
        @RequestBody request: GalleryUpdateRequest,
        auth: Authentication
    ): GalleryImageDto {
        val image = getOwnImage(auth.userId(), imageId)
        image.caption = request.caption
        galleryRepository.save(image)
        return image.toDto()
    }

    @PutMapping("/providers/me/gallery/order")
    @Transactional
    fun reorderImages(
        @RequestBody request: GalleryReorderRequest,
        auth: Authentication
    ): List<GalleryImageDto> {
        val providerId = auth.userId()
        val images = galleryRepository.findByProviderUserIdOrderBySortOrder(providerId)
        val imageMap = images.associateBy { it.id }

        request.imageIds.forEachIndexed { index, id ->
            val image = imageMap[id] ?: throw NotFoundException("Image", id)
            image.sortOrder = index
        }

        galleryRepository.saveAll(images)
        return images.sortedBy { it.sortOrder }.map { it.toDto() }
    }

    @DeleteMapping("/providers/me/gallery/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun deleteImage(@PathVariable imageId: UUID, auth: Authentication) {
        val image = getOwnImage(auth.userId(), imageId)
        image.storageKey?.let { storageService.delete(it) }
        galleryRepository.delete(image)
    }

    private fun getOwnImage(providerId: UUID, imageId: UUID): ProviderGalleryImage {
        val image = galleryRepository.findById(imageId)
            .orElseThrow { NotFoundException("Image", imageId) }
        if (image.provider.userId != providerId) {
            throw ForbiddenException("Not your image")
        }
        return image
    }

    private fun ProviderGalleryImage.toDto() = GalleryImageDto(
        id = id,
        url = url,
        caption = caption,
        sortOrder = sortOrder
    )
}
