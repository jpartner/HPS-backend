package com.hps.persistence.wallet

import com.hps.domain.wallet.TokenAccount
import com.hps.domain.wallet.TokenAccountType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface TokenAccountRepository : JpaRepository<TokenAccount, UUID> {

    fun findByUserIdAndTenantId(userId: UUID, tenantId: UUID): TokenAccount?

    @Query(value = "SELECT * FROM token_accounts WHERE id = :id FOR UPDATE", nativeQuery = true)
    fun findByIdForUpdate(id: UUID): TokenAccount?

    @Query(value = "SELECT * FROM token_accounts WHERE user_id = :userId AND tenant_id = :tenantId FOR UPDATE", nativeQuery = true)
    fun findByUserIdForUpdate(userId: UUID, tenantId: UUID): TokenAccount?

    @Query("SELECT a FROM TokenAccount a WHERE a.tenantId = :tenantId AND a.accountType = :type")
    fun findByTenantIdAndAccountType(tenantId: UUID, type: TokenAccountType): TokenAccount?

    @Query(value = "SELECT * FROM token_accounts WHERE tenant_id = :tenantId AND account_type = 'SYSTEM' FOR UPDATE", nativeQuery = true)
    fun findSystemAccountForUpdate(tenantId: UUID): TokenAccount?

    fun findByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<TokenAccount>
}
