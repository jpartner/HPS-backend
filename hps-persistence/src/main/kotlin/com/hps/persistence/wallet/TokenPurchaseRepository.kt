package com.hps.persistence.wallet

import com.hps.domain.wallet.TokenPurchase
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TokenPurchaseRepository : JpaRepository<TokenPurchase, UUID> {

    fun findByAccountIdOrderByCreatedAtDesc(accountId: UUID): List<TokenPurchase>
}
