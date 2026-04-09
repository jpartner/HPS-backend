package com.hps.api.admin

import com.hps.api.wallet.AdminAccountDto
import com.hps.api.wallet.AdminGrantRequest
import com.hps.api.wallet.TransactionDto
import com.hps.api.wallet.WalletService
import com.hps.common.tenant.TenantContext
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/wallets")
class AdminWalletController(
    private val walletService: WalletService
) {

    @GetMapping
    fun listAccounts(): List<AdminAccountDto> {
        return walletService.listAccounts(TenantContext.require())
    }

    @PostMapping("/{accountId}/grant")
    fun grantTokens(
        @PathVariable accountId: UUID,
        @RequestBody request: AdminGrantRequest
    ): TransactionDto {
        return walletService.adminGrant(accountId, request)
    }
}
