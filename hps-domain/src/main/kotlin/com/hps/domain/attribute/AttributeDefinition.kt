package com.hps.domain.attribute

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "attribute_definitions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["domain", "key"])]
)
class AttributeDefinition(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 100)
    val domain: String,

    @Column(name = "key", nullable = false, length = 100)
    val key: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    var dataType: AttributeDataType = AttributeDataType.TEXT,

    @Column(name = "is_required", nullable = false)
    var isRequired: Boolean = false,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var options: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var validation: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "attribute", cascade = [CascadeType.ALL], orphanRemoval = true)
    val translations: MutableSet<AttributeDefinitionTranslation> = mutableSetOf()
)

@Entity
@Table(
    name = "attribute_definition_translations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["attribute_id", "lang"])]
)
class AttributeDefinitionTranslation(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    val attribute: AttributeDefinition,

    @Column(name = "lang", nullable = false, length = 5)
    val lang: String,

    @Column(nullable = false)
    val label: String,

    @Column(length = 500)
    val hint: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "option_labels", columnDefinition = "JSONB")
    val optionLabels: String? = null
)

enum class AttributeDataType {
    TEXT, NUMBER, BOOLEAN, SELECT, MULTI_SELECT
}
