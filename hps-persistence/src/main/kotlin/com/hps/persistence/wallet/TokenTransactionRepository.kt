package com.hps.persistence.wallet

import com.hps.domain.wallet.TokenTransaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TokenTransactionRepository : JpaRepository<TokenTransaction, UUID> {

    fun findByAccountIdOrderByCreatedAtDesc(accountId: UUID, pageable: Pageable): Page<TokenTransaction>

    fun findByAccountIdOrderByCreatedAtDesc(accountId: UUID): List<TokenTransaction>
}
