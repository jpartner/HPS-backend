package com.hps.api.auth

import com.hps.common.tenant.TenantContext
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): TokenResponse =
        authService.register(request)

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): TokenResponse =
        authService.login(request)

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): TokenResponse =
        authService.refresh(request)

    @GetMapping("/handle-available")
    fun checkHandle(@RequestParam handle: String): HandleAvailableResponse =
        authService.checkHandleAvailable(handle, TenantContext.get())
}
