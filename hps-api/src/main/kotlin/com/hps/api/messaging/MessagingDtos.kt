package com.hps.api.messaging

import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

data class ConversationDto(
    val id: UUID,
    val otherParticipant: ParticipantDto,
    val conversationType: String,
    val topic: String?,
    val lastMessage: MessageSummaryDto?,
    val updatedAt: Instant
)

data class ParticipantDto(
    val id: UUID,
    val email: String,
    val name: String?,
    val role: String
)

data class MessageDto(
    val id: UUID,
    val senderId: UUID,
    val senderName: String?,
    val content: String,
    val isRead: Boolean,
    val createdAt: Instant
)

data class MessageSummaryDto(
    val content: String,
    val senderId: UUID,
    val createdAt: Instant,
    val isRead: Boolean
)

data class CreateConversationRequest(
    val participantId: UUID,
    val topic: String? = null,
    @field:NotBlank
    val initialMessage: String
)

data class SendMessageRequest(
    @field:NotBlank
    val content: String
)

data class BlockUserRequest(
    val userId: UUID
)

data class BlockedUserDto(
    val id: UUID,
    val email: String,
    val name: String?,
    val blockedAt: Instant
)
