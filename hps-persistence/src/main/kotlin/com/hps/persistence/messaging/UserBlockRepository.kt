package com.hps.persistence.messaging

import com.hps.domain.messaging.UserBlock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserBlockRepository : JpaRepository<UserBlock, UUID> {

    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM UserBlock b
        WHERE (b.blocker.id = :userId1 AND b.blocked.id = :userId2)
           OR (b.blocker.id = :userId2 AND b.blocked.id = :userId1)
    """)
    fun isBlocked(userId1: UUID, userId2: UUID): Boolean

    fun findByBlockerId(blockerId: UUID): List<UserBlock>

    fun findByBlockerIdAndBlockedId(blockerId: UUID, blockedId: UUID): UserBlock?
}
