package com.hps.domain.messaging

import com.hps.domain.user.User
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "conversations")
class Conversation(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant1_id", nullable = false)
    val participant1: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant2_id", nullable = false)
    val participant2: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type", nullable = false)
    val conversationType: ConversationType = ConversationType.CUSTOMER_PROVIDER,

    @Column(length = 255)
    var topic: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

enum class ConversationType {
    CUSTOMER_PROVIDER,
    PROVIDER_ADMIN,
    ADMIN_CUSTOMER
}

@Entity
@Table(name = "messages")
class Message(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    val conversation: Conversation,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    val sender: User,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(
    name = "user_blocks",
    uniqueConstraints = [UniqueConstraint(columnNames = ["blocker_id", "blocked_id"])]
)
class UserBlock(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    val blocker: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    val blocked: User,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
