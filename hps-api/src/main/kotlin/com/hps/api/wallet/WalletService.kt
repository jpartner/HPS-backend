package com.hps.api.wallet

import com.hps.common.exception.BadRequestException
import com.hps.common.exception.InsufficientBalanceException
import com.hps.common.exception.NotFoundException
import com.hps.domain.wallet.*
import com.hps.persistence.user.UserRepository
import com.hps.persistence.wallet.TokenAccountRepository
import com.hps.persistence.wallet.TokenPurchaseRepository
import com.hps.persistence.wallet.TokenTransactionRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class WalletService(
    private val accountRepository: TokenAccountRepository,
    private val purchaseRepository: TokenPurchaseRepository,
    private val transactionRepository: TokenTransactionRepository,
    private val userRepository: UserRepository
) {

    // ── Account management ──

    @Transactional
    fun getOrCreateAccount(userId: UUID, tenantId: UUID): TokenAccount {
        accountRepository.findByUserIdAndTenantId(userId, tenantId)?.let { return it }

        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User", userId) }

        return try {
            accountRepository.save(
                TokenAccount(user = user, tenantId = tenantId, accountType = TokenAccountType.USER)
            )
        } catch (e: DataIntegrityViolationException) {
            // Race condition: another thread created it first
            accountRepository.findByUserIdAndTenantId(userId, tenantId)!!
        }
    }

    @Transactional
    fun getOrCreateSystemAccount(tenantId: UUID): TokenAccount {
        accountRepository.findByTenantIdAndAccountType(tenantId, TokenAccountType.SYSTEM)?.let { return it }

        return try {
            accountRepository.save(
                TokenAccount(user = null, tenantId = tenantId, accountType = TokenAccountType.SYSTEM)
            )
        } catch (e: DataIntegrityViolationException) {
            accountRepository.findByTenantIdAndAccountType(tenantId, TokenAccountType.SYSTEM)!!
        }
    }

    // ── Balance queries ──

    @Transactional
    fun getBalance(userId: UUID, tenantId: UUID): BalanceDto {
        val account = getOrCreateAccount(userId, tenantId)
        return account.toBalanceDto()
    }

    @Transactional
    fun getTransactions(userId: UUID, tenantId: UUID, page: Int, size: Int): Page<TransactionDto> {
        val account = getOrCreateAccount(userId, tenantId)
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(
            account.id, PageRequest.of(page, size)
        ).map { it.toDto() }
    }

    // ── Purchase flow ──

    @Transactional
    fun initiatePurchase(userId: UUID, tenantId: UUID, request: PurchaseRequest): PurchaseDto {
        if (request.amount <= BigDecimal.ZERO) {
            throw BadRequestException("Purchase amount must be positive")
        }
        val account = getOrCreateAccount(userId, tenantId)
        val purchase = purchaseRepository.save(
            TokenPurchase(
                account = account,
                tenantId = tenantId,
                amount = request.amount,
                paymentProvider = request.paymentProvider
            )
        )
        return purchase.toDto()
    }

    @Transactional
    fun confirmPurchase(purchaseId: UUID, request: ConfirmPurchaseRequest?): PurchaseDto {
        val purchase = purchaseRepository.findById(purchaseId)
            .orElseThrow { NotFoundException("Purchase", purchaseId) }

        if (purchase.status == TokenPurchaseStatus.COMPLETED) {
            return purchase.toDto() // Idempotent
        }
        if (purchase.status != TokenPurchaseStatus.PENDING) {
            throw BadRequestException("Purchase is ${purchase.status}, cannot confirm")
        }

        // Lock the account and credit
        val account = accountRepository.findByIdForUpdate(purchase.account.id)
            ?: throw NotFoundException("Account", purchase.account.id)

        request?.paymentReference?.let { purchase.paymentReference = it }
        purchase.status = TokenPurchaseStatus.COMPLETED

        creditAccount(account, purchase.amount, TokenTransactionType.PURCHASE_CREDIT,
            "PURCHASE", purchase.id, "Token purchase of ${purchase.amount}")

        purchaseRepository.save(purchase)
        return purchase.toDto()
    }

    // ── Spend (called by other services) ──

    @Transactional
    fun spend(userId: UUID, tenantId: UUID, amount: BigDecimal, referenceType: String, referenceId: UUID, description: String?): TransactionDto {
        if (amount <= BigDecimal.ZERO) {
            throw BadRequestException("Spend amount must be positive")
        }
        val account = accountRepository.findByUserIdForUpdate(userId, tenantId)
            ?: throw BadRequestException("No token account found")

        return debitAccount(account, amount, TokenTransactionType.SPEND,
            referenceType, referenceId, description).toDto()
    }

    // ── Admin operations ──

    @Transactional
    fun adminGrant(accountId: UUID, request: AdminGrantRequest): TransactionDto {
        if (request.amount <= BigDecimal.ZERO) {
            throw BadRequestException("Grant amount must be positive")
        }
        val account = accountRepository.findByIdForUpdate(accountId)
            ?: throw NotFoundException("Account", accountId)

        return creditAccount(account, request.amount, TokenTransactionType.ADMIN_GRANT,
            null, null, request.description ?: "Admin grant").toDto()
    }

    @Transactional(readOnly = true)
    fun listAccounts(tenantId: UUID): List<AdminAccountDto> {
        return accountRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).map { account ->
            AdminAccountDto(
                accountId = account.id,
                userId = account.user?.id,
                email = account.user?.email,
                balance = account.balance,
                accountType = account.accountType.name,
                createdAt = account.createdAt
            )
        }
    }

    // ── Internal helpers ──

    private fun creditAccount(
        account: TokenAccount, amount: BigDecimal, type: TokenTransactionType,
        referenceType: String?, referenceId: UUID?, description: String?
    ): TokenTransaction {
        account.balance = account.balance + amount
        account.updatedAt = Instant.now()
        accountRepository.save(account)

        return transactionRepository.save(
            TokenTransaction(
                account = account, tenantId = account.tenantId, type = type,
                amount = amount, balanceAfter = account.balance,
                referenceType = referenceType, referenceId = referenceId, description = description
            )
        )
    }

    private fun debitAccount(
        account: TokenAccount, amount: BigDecimal, type: TokenTransactionType,
        referenceType: String?, referenceId: UUID?, description: String?
    ): TokenTransaction {
        if (account.balance < amount) {
            throw InsufficientBalanceException("Insufficient balance: have ${account.balance}, need $amount")
        }
        account.balance = account.balance - amount
        account.updatedAt = Instant.now()
        accountRepository.save(account)

        return transactionRepository.save(
            TokenTransaction(
                account = account, tenantId = account.tenantId, type = type,
                amount = amount, balanceAfter = account.balance,
                referenceType = referenceType, referenceId = referenceId, description = description
            )
        )
    }

    // ── Mappers ──

    private fun TokenAccount.toBalanceDto() = BalanceDto(
        accountId = id, balance = balance, accountType = accountType.name
    )

    private fun TokenTransaction.toDto() = TransactionDto(
        id = id, type = type.name, amount = amount, balanceAfter = balanceAfter,
        referenceType = referenceType, referenceId = referenceId,
        description = description, createdAt = createdAt
    )

    private fun TokenPurchase.toDto() = PurchaseDto(
        id = id, amount = amount, status = status.name,
        paymentProvider = paymentProvider, paymentReference = paymentReference,
        createdAt = createdAt
    )
}
