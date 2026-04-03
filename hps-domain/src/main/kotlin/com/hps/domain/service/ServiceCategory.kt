package com.hps.domain.service

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "service_categories")
class ServiceCategory(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    val parent: ServiceCategory? = null,

    @Column(length = 100)
    var icon: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "category", cascade = [CascadeType.ALL], orphanRemoval = true)
    val translations: MutableSet<ServiceCategoryTranslation> = mutableSetOf(),

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], orphanRemoval = true)
    val children: MutableList<ServiceCategory> = mutableListOf()
)

@Entity
@Table(
    name = "service_category_translations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["category_id", "lang"])]
)
class ServiceCategoryTranslation(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: ServiceCategory,

    @Column(name = "lang", nullable = false, length = 5)
    val lang: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null
)
