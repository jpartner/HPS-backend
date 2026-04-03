package com.hps.api.provider

import java.util.UUID

data class GalleryImageDto(
    val id: UUID,
    val url: String,
    val caption: String?,
    val sortOrder: Int
)

data class GalleryReorderRequest(
    val imageIds: List<UUID>
)

data class GalleryUpdateRequest(
    val caption: String?
)

data class GalleryAddUrlRequest(
    val url: String,
    val caption: String? = null
)
