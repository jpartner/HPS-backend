package com.hps.domain.wallet

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "token_purchases")
class TokenPurchase(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: TokenAccount,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(nullable = false, precision = 12, scale = 2)
    val amount: BigDecimal,

    @Column(name = "payment_provider", length = 50)
    var paymentProvider: String? = null,

    @Column(name = "payment_reference", length = 255)
    var paymentReference: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TokenPurchaseStatus = TokenPurchaseStatus.PENDING,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

enum class TokenPurchaseStatus {
    PENDING, COMPLETED, FAILED, REFUNDED
}
