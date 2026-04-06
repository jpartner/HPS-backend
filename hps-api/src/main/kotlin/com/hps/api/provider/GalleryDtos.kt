package com.hps.api.provider

import java.time.Instant
import java.util.UUID

// Public view (for profile pages — uses CDN URL when available)
data class GalleryImageDto(
    val id: UUID,
    val url: String,
    val thumbnailUrl: String?,
    val caption: String?,
    val sortOrder: Int
)

// Full media view (for provider's own management + admin)
data class MediaDto(
    val id: UUID,
    val url: String,
    val thumbnailUrl: String?,
    val caption: String?,
    val sortOrder: Int,
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

data class GalleryReorderRequest(
    val imageIds: List<UUID>
)

data class MediaUpdateRequest(
    val caption: String? = null,
    val blurRequested: Boolean? = null,
    val isPrivate: Boolean? = null
)

data class GalleryAddUrlRequest(
    val url: String,
    val caption: String? = null,
    val mediaType: String = "GALLERY",
    val isPrivate: Boolean = false,
    val blurRequested: Boolean = false
)

// Keep old name
@Suppress("unused")
typealias GalleryUpdateRequest = MediaUpdateRequest
