package com.hps.persistence.user

import com.hps.domain.user.User
import com.hps.domain.user.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {

    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean

    fun findByEmailAndTenantId(email: String, tenantId: UUID): User?

    fun existsByEmailAndTenantId(email: String, tenantId: UUID): Boolean

    fun findByTenantId(tenantId: UUID): List<User>

    fun findByTenantIdAndRole(tenantId: UUID, role: UserRole): List<User>

    fun countByTenantId(tenantId: UUID): Long

    fun findByHandleAndTenantId(handle: String, tenantId: UUID): User?

    fun existsByHandleAndTenantId(handle: String, tenantId: UUID): Boolean
}
