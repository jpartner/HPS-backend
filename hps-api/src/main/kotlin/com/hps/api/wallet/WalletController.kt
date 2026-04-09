package com.hps.api.wallet

import com.hps.api.auth.userId
import com.hps.common.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/wallet")
class WalletController(
    private val walletService: WalletService
) {

    @GetMapping("/balance")
    fun getBalance(auth: Authentication): BalanceDto {
        return walletService.getBalance(auth.userId(), TenantContext.require())
    }

    @GetMapping("/transactions")
    fun getTransactions(
        auth: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Page<TransactionDto> {
        return walletService.getTransactions(auth.userId(), TenantContext.require(), page, size)
    }

    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    fun initiatePurchase(
        @RequestBody request: PurchaseRequest,
        auth: Authentication
    ): PurchaseDto {
        return walletService.initiatePurchase(auth.userId(), TenantContext.require(), request)
    }

    @PostMapping("/purchase/{id}/confirm")
    fun confirmPurchase(
        @PathVariable id: java.util.UUID,
        @RequestBody(required = false) request: ConfirmPurchaseRequest?
    ): PurchaseDto {
        return walletService.confirmPurchase(id, request)
    }
}
