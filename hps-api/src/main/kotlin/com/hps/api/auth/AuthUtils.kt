package com.hps.api.auth

import com.hps.common.exception.ForbiddenException
import com.hps.domain.user.UserRole
import org.springframework.security.core.Authentication
import java.util.UUID

fun Authentication.userId(): UUID = UUID.fromString(principal as String)

fun Authentication.role(): UserRole {
    val authority = authorities.firstOrNull()?.authority ?: "ROLE_CLIENT"
    return UserRole.valueOf(authority.removePrefix("ROLE_"))
}

fun Authentication.isAdmin(): Boolean = role() == UserRole.ADMIN

fun Authentication.isProvider(): Boolean = role() == UserRole.PROVIDER

fun Authentication.requireOwnerOrAdmin(resourceOwnerId: UUID) {
    if (!isAdmin() && userId() != resourceOwnerId) {
        throw ForbiddenException("You do not have access to this resource")
    }
}
