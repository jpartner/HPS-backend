package com.hps.domain.reference

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "reference_lists")
class ReferenceList(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 100)
    var key: String,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "referenceList", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    val items: MutableList<ReferenceListItem> = mutableListOf()
)

@Entity
@Table(name = "reference_list_items")
class ReferenceListItem(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_list_id", nullable = false)
    val referenceList: ReferenceList,

    @Column(nullable = false, length = 100)
    var value: String,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @OneToMany(mappedBy = "item", cascade = [CascadeType.ALL], orphanRemoval = true)
    val translations: MutableSet<ReferenceListItemTranslation> = mutableSetOf()
)

@Entity
@Table(
    name = "reference_list_item_translations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["item_id", "lang"])]
)
class ReferenceListItemTranslation(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    val item: ReferenceListItem,

    @Column(nullable = false, length = 5)
    val lang: String,

    @Column(nullable = false, length = 255)
    val label: String
)
