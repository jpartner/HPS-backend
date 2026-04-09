package com.hps.api.wallet

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class BalanceDto(
    val accountId: UUID,
    val balance: BigDecimal,
    val accountType: String
)

data class TransactionDto(
    val id: UUID,
    val type: String,
    val amount: BigDecimal,
    val balanceAfter: BigDecimal,
    val referenceType: String?,
    val referenceId: UUID?,
    val description: String?,
    val createdAt: Instant
)

data class PurchaseRequest(
    val amount: BigDecimal,
    val paymentProvider: String? = null
)

data class PurchaseDto(
    val id: UUID,
    val amount: BigDecimal,
    val status: String,
    val paymentProvider: String?,
    val paymentReference: String?,
    val createdAt: Instant
)

data class ConfirmPurchaseRequest(
    val paymentReference: String? = null
)

data class AdminGrantRequest(
    val amount: BigDecimal,
    val description: String? = null
)

data class AdminAccountDto(
    val accountId: UUID,
    val userId: UUID?,
    val email: String?,
    val balance: BigDecimal,
    val accountType: String,
    val createdAt: Instant
)
