package com.hps.domain.user

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notification_preferences")
class NotificationPreferences(
    @Id
    var userId: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    val user: User,

    @Column(name = "telegram_chat_id", length = 100)
    var telegramChatId: String? = null,

    @Column(name = "push_enabled", nullable = false)
    var pushEnabled: Boolean = false,

    @Column(name = "email_enabled", nullable = false)
    var emailEnabled: Boolean = true,

    @Column(name = "message_notify", nullable = false)
    var messageNotify: Boolean = true,

    @Column(name = "booking_notify", nullable = false)
    var bookingNotify: Boolean = true,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
