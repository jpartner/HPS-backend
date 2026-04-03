package com.hps.persistence.messaging

import com.hps.domain.messaging.Message
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MessageRepository : JpaRepository<Message, UUID> {

    fun findByConversationIdOrderByCreatedAtDesc(conversationId: UUID, pageable: Pageable): Page<Message>

    @Modifying
    @Query("""
        UPDATE Message m SET m.isRead = true
        WHERE m.conversation.id = :conversationId
        AND m.sender.id != :userId
        AND m.isRead = false
    """)
    fun markAsRead(conversationId: UUID, userId: UUID): Int

    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.conversation.id IN (
            SELECT c.id FROM Conversation c
            WHERE c.participant1.id = :userId OR c.participant2.id = :userId
        )
        AND m.sender.id != :userId
        AND m.isRead = false
    """)
    fun countUnread(userId: UUID): Long
}
