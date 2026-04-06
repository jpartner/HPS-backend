package com.hps.domain.messaging

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class ConversationArchiveId(
    @Column(name = "user_id")
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "conversation_id")
    val conversationId: UUID = UUID.randomUUID()
) : Serializable

@Entity
@Table(name = "conversation_archives")
class ConversationArchive(
    @EmbeddedId
    val id: ConversationArchiveId,

    @Column(name = "archived_at", nullable = false, updatable = false)
    val archivedAt: Instant = Instant.now()
)
