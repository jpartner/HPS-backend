package com.hps.domain.wallet

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "token_transactions")
class TokenTransaction(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: TokenAccount,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val type: TokenTransactionType,

    @Column(nullable = false, precision = 12, scale = 2)
    val amount: BigDecimal,

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    val balanceAfter: BigDecimal,

    @Column(name = "reference_type", length = 50)
    val referenceType: String? = null,

    @Column(name = "reference_id")
    val referenceId: UUID? = null,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

enum class TokenTransactionType {
    PURCHASE_CREDIT, SPEND, REFUND, ADMIN_GRANT, TRANSFER_IN, TRANSFER_OUT
}
