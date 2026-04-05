package com.hps.persistence.messaging

import com.hps.domain.messaging.Conversation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ConversationRepository : JpaRepository<Conversation, UUID> {

    fun findByTenantId(tenantId: UUID): List<Conversation>

    @Query("""
        SELECT c FROM Conversation c
        WHERE c.participant1.id = :userId OR c.participant2.id = :userId
        ORDER BY c.updatedAt DESC
    """)
    fun findByParticipant(userId: UUID): List<Conversation>

    @Query("""
        SELECT c FROM Conversation c
        WHERE (c.participant1.id = :userId1 AND c.participant2.id = :userId2)
           OR (c.participant1.id = :userId2 AND c.participant2.id = :userId1)
    """)
    fun findByParticipants(userId1: UUID, userId2: UUID): List<Conversation>
}
