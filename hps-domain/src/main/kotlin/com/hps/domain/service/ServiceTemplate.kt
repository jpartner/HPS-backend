package com.hps.domain.service

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "service_templates")
class ServiceTemplate(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: ServiceCategory,

    @Column(nullable = false, unique = true, length = 100)
    val slug: String,

    @Column(name = "default_duration_minutes")
    var defaultDurationMinutes: Int? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "template", cascade = [CascadeType.ALL], orphanRemoval = true)
    val translations: MutableSet<ServiceTemplateTranslation> = mutableSetOf()
)

@Entity
@Table(
    name = "service_template_translations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["template_id", "lang"])]
)
class ServiceTemplateTranslation(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    val template: ServiceTemplate,

    @Column(name = "lang", nullable = false, length = 5)
    val lang: String,

    @Column(nullable = false)
    val title: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null
)
