package com.hps.domain.user

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "provider_gallery_images")
class ProviderGalleryImage(
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

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
