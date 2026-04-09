package com.hps.domain.wallet

import com.hps.domain.user.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "token_accounts")
class TokenAccount(
    @Id
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    val user: User? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(nullable = false, precision = 12, scale = 2)
    var balance: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    val accountType: TokenAccountType = TokenAccountType.USER,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

enum class TokenAccountType {
    USER, SYSTEM
}
