package com.hps.api.auth

import com.hps.common.exception.ForbiddenException
import com.hps.common.tenant.TenantContext
import com.hps.domain.user.UserRole
import org.springframework.security.core.Authentication
import java.util.UUID

fun Authentication.userId(): UUID = UUID.fromString(principal as String)

fun Authentication.role(): UserRole {
    val authority = authorities.firstOrNull()?.authority ?: "ROLE_CLIENT"
    return UserRole.valueOf(authority.removePrefix("ROLE_"))
}

fun Authentication.isAdmin(): Boolean = role() == UserRole.ADMIN || role() == UserRole.SUPER_ADMIN

fun Authentication.isSuperAdmin(): Boolean = role() == UserRole.SUPER_ADMIN

fun Authentication.isProvider(): Boolean = role() == UserRole.PROVIDER

fun Authentication.tenantId(): UUID = TenantContext.get()
    ?: throw ForbiddenException("No tenant context")

fun Authentication.requireOwnerOrAdmin(resourceOwnerId: UUID) {
    if (!isAdmin() && userId() != resourceOwnerId) {
        throw ForbiddenException("You do not have access to this resource")
    }
}
