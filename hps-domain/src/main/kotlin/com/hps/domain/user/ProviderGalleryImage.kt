package com.hps.domain.user

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "provider_media")
class ProviderMedia(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    val provider: ProviderProfile,

    @Column(nullable = false, length = 500)
    var url: String,

    @Column(name = "storage_key", length = 500)
    val storageKey: String? = null,

    @Column(length = 500)
    var caption: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    val mediaType: MediaType = MediaType.GALLERY,

    @Column(name = "content_type", length = 50)
    val contentType: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    var approvalStatus: MediaApprovalStatus = MediaApprovalStatus.PENDING,

    @Column(name = "review_note", columnDefinition = "TEXT")
    var reviewNote: String? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    var reviewedBy: User? = null,

    @Column(name = "is_private", nullable = false)
    var isPrivate: Boolean = false,

    @Column(name = "blur_requested", nullable = false)
    var blurRequested: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    val isVideo: Boolean get() = contentType?.startsWith("video/") == true
}

enum class MediaType {
    GALLERY, VERIFICATION, AVATAR
}

enum class MediaApprovalStatus {
    PENDING, APPROVED, REJECTED
}

// Keep type alias for backward compatibility in code that references the old name
@Suppress("unused")
typealias ProviderGalleryImage = ProviderMedia
