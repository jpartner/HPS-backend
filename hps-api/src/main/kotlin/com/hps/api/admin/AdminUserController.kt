package com.hps.api.admin

import com.hps.api.auth.isSuperAdmin
import com.hps.common.exception.ForbiddenException
import com.hps.common.exception.NotFoundException
import com.hps.common.tenant.TenantContext
import com.hps.domain.user.UserRole
import com.hps.persistence.user.UserRepository
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

// --- DTOs ---

data class AdminUserDto(
    val id: UUID,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: Instant,
    val avatarUrl: String?
)

data class UpdateUserStatusRequest(
    val isActive: Boolean
)

data class UpdateUserRoleRequest(
    val role: UserRole
)

// --- Controller ---

@RestController
@RequestMapping("/api/v1/admin/users")
class AdminUserController(
    private val userRepository: UserRepository
) {

    @GetMapping
    @Transactional(readOnly = true)
    fun listUsers(@RequestParam(required = false) role: UserRole?): List<AdminUserDto> {
        val tenantId = TenantContext.require()
        val users = if (role != null) {
            userRepository.findByTenantIdAndRole(tenantId, role)
        } else {
            userRepository.findByTenantId(tenantId)
        }
        return users.map { user ->
            AdminUserDto(
                id = user.id,
                email = user.email,
                firstName = user.profile?.firstName,
                lastName = user.profile?.lastName,
                role = user.role,
                isActive = user.isActive,
                createdAt = user.createdAt,
                avatarUrl = user.avatarUrl
            )
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    fun getUser(@PathVariable id: UUID): AdminUserDto {
        val tenantId = TenantContext.require()
        val user = userRepository.findById(id)
            .orElseThrow { NotFoundException("User", id) }
        if (user.tenantId != tenantId) {
            throw NotFoundException("User", id)
        }
        return AdminUserDto(
            id = user.id,
            email = user.email,
            firstName = user.profile?.firstName,
            lastName = user.profile?.lastName,
            role = user.role,
            isActive = user.isActive,
            createdAt = user.createdAt,
            avatarUrl = user.avatarUrl
        )
    }

    @PutMapping("/{id}/status")
    @Transactional
    fun updateUserStatus(
        @PathVariable id: UUID,
        @RequestBody request: UpdateUserStatusRequest
    ): AdminUserDto {
        val tenantId = TenantContext.require()
        val user = userRepository.findById(id)
            .orElseThrow { NotFoundException("User", id) }
        if (user.tenantId != tenantId) {
            throw NotFoundException("User", id)
        }
        user.isActive = request.isActive
        user.updatedAt = Instant.now()
        val saved = userRepository.save(user)
        return AdminUserDto(
            id = saved.id,
            email = saved.email,
            firstName = saved.profile?.firstName,
            lastName = saved.profile?.lastName,
            role = saved.role,
            isActive = saved.isActive,
            createdAt = saved.createdAt,
            avatarUrl = saved.avatarUrl
        )
    }

    @PutMapping("/{id}/role")
    @Transactional
    fun updateUserRole(
        @PathVariable id: UUID,
        @RequestBody request: UpdateUserRoleRequest,
        auth: Authentication
    ): AdminUserDto {
        val tenantId = TenantContext.require()
        val user = userRepository.findById(id)
            .orElseThrow { NotFoundException("User", id) }
        if (user.tenantId != tenantId) {
            throw NotFoundException("User", id)
        }
        if (request.role == UserRole.ADMIN && !auth.isSuperAdmin()) {
            throw ForbiddenException("Only SUPER_ADMIN can assign ADMIN role")
        }
        if (request.role == UserRole.SUPER_ADMIN && !auth.isSuperAdmin()) {
            throw ForbiddenException("Only SUPER_ADMIN can assign SUPER_ADMIN role")
        }
        user.role = request.role
        user.updatedAt = Instant.now()
        val saved = userRepository.save(user)
        return AdminUserDto(
            id = saved.id,
            email = saved.email,
            firstName = saved.profile?.firstName,
            lastName = saved.profile?.lastName,
            role = saved.role,
            isActive = saved.isActive,
            createdAt = saved.createdAt,
            avatarUrl = saved.avatarUrl
        )
    }
}
