package com.hps.geo.seed

import com.hps.domain.user.User
import com.hps.domain.user.UserProfile
import com.hps.domain.user.UserRole
import com.hps.persistence.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Seeds a SUPER_ADMIN user on first startup if none exists.
 *
 * Configure via environment variables:
 *   HPS_SUPERADMIN_EMAIL    (default: admin@hps.local)
 *   HPS_SUPERADMIN_PASSWORD (default: changeme123)
 *
 * Runs before other seeders (Order 0) so the super admin
 * exists before any tenant-scoped data is created.
 */
@Component
@Order(0)
class SuperAdminSeeder(
    private val userRepository: UserRepository
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(SuperAdminSeeder::class.java)
    private val passwordEncoder = BCryptPasswordEncoder()

    @Transactional
    override fun run(args: ApplicationArguments) {
        // Check if any SUPER_ADMIN exists
        val existing = userRepository.findAll().any { it.role == UserRole.SUPER_ADMIN }
        if (existing) {
            log.info("SUPER_ADMIN already exists, skipping")
            return
        }

        val email = System.getenv("HPS_SUPERADMIN_EMAIL") ?: "admin@hps.local"
        val password = System.getenv("HPS_SUPERADMIN_PASSWORD") ?: "changeme123"

        // Check if this email is already taken
        if (userRepository.findByEmail(email) != null) {
            log.warn("User '$email' already exists but is not SUPER_ADMIN. Promoting.")
            val user = userRepository.findByEmail(email)!!
            user.role = UserRole.SUPER_ADMIN
            userRepository.save(user)
            log.info("Promoted '$email' to SUPER_ADMIN")
            return
        }

        val user = User(
            email = email,
            tenantId = null, // SUPER_ADMIN is global, no tenant
            passwordHash = passwordEncoder.encode(password),
            role = UserRole.SUPER_ADMIN
        )
        user.profile = UserProfile(
            user = user,
            firstName = "Super",
            lastName = "Admin"
        )

        userRepository.save(user)
        log.info("Created SUPER_ADMIN: $email")

        if (password == "changeme123") {
            log.warn("!!! SUPER_ADMIN using default password. Set HPS_SUPERADMIN_PASSWORD env var !!!")
        }
    }
}
