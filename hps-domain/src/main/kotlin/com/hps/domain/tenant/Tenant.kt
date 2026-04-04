package com.hps.domain.tenant

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "tenants")
class Tenant(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 100)
    var slug: String,

    @Column(nullable = false)
    var name: String,

    @Column(length = 255)
    var domain: String? = null,

    @Column(name = "default_lang", nullable = false, length = 5)
    var defaultLang: String = "en",

    @Column(name = "supported_langs", nullable = false, length = 100)
    var supportedLangs: String = "en",

    @Column(name = "default_currency", nullable = false, length = 3)
    var defaultCurrency: String = "EUR",

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB", nullable = false)
    var settings: String = "{}",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
