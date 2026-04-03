package com.hps.api.messaging

import com.hps.common.exception.BadRequestException
import com.hps.common.exception.ForbiddenException
import com.hps.common.exception.NotFoundException
import com.hps.domain.messaging.Conversation
import com.hps.domain.messaging.ConversationType
import com.hps.domain.messaging.Message
import com.hps.domain.messaging.UserBlock
import com.hps.domain.user.User
import com.hps.domain.user.UserRole
import com.hps.persistence.booking.BookingRepository
import com.hps.persistence.messaging.ConversationRepository
import com.hps.persistence.messaging.MessageRepository
import com.hps.persistence.messaging.UserBlockRepository
import com.hps.persistence.user.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class MessagingService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val blockRepository: UserBlockRepository,
    private val userRepository: UserRepository,
    private val bookingRepository: BookingRepository
) {
    fun listConversations(userId: UUID): List<ConversationDto> {
        val conversations = conversationRepository.findByParticipant(userId)
        return conversations.map { it.toDto(userId) }
    }

    fun getMessages(userId: UUID, conversationId: UUID, page: Int, size: Int): List<MessageDto> {
        val conversation = conversationRepository.findById(conversationId)
            .orElseThrow { NotFoundException("Conversation", conversationId) }

        requireParticipant(conversation, userId)

        return messageRepository
            .findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(page, size))
            .content
            .map { it.toDto() }
    }

    @Transactional
    fun createConversation(
        userId: UUID,
        userRole: UserRole,
        request: CreateConversationRequest
    ): ConversationDto {
        val initiator = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User", userId) }
        val target = userRepository.findById(request.participantId)
            .orElseThrow { NotFoundException("User", request.participantId) }

        if (userId == request.participantId) {
            throw BadRequestException("Cannot create conversation with yourself")
        }

        // Check blocking
        if (blockRepository.isBlocked(userId, request.participantId)) {
            throw ForbiddenException("Cannot communicate with this user")
        }

        // Determine conversation type and enforce rules
        val conversationType = resolveConversationType(initiator, target, userRole)
        enforceConversationRules(initiator, target, userRole, conversationType)

        // Check if conversation already exists (unless it's a new topic-based admin conversation)
        val existing = conversationRepository.findByParticipants(userId, request.participantId)
        val conversation = if (existing.isNotEmpty() && conversationType != ConversationType.PROVIDER_ADMIN) {
            existing.first()
        } else {
            conversationRepository.save(
                Conversation(
                    participant1 = initiator,
                    participant2 = target,
                    conversationType = conversationType,
                    topic = request.topic
                )
            )
        }

        // Send initial message
        messageRepository.save(
            Message(
                conversation = conversation,
                sender = initiator,
                content = request.initialMessage
            )
        )

        conversation.updatedAt = Instant.now()
        conversationRepository.save(conversation)

        return conversation.toDto(userId)
    }

    @Transactional
    fun sendMessage(userId: UUID, conversationId: UUID, request: SendMessageRequest): MessageDto {
        val conversation = conversationRepository.findById(conversationId)
            .orElseThrow { NotFoundException("Conversation", conversationId) }

        requireParticipant(conversation, userId)

        val otherId = otherParticipantId(conversation, userId)
        if (blockRepository.isBlocked(userId, otherId)) {
            throw ForbiddenException("Cannot communicate with this user")
        }

        val sender = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User", userId) }

        val message = messageRepository.save(
            Message(
                conversation = conversation,
                sender = sender,
                content = request.content
            )
        )

        conversation.updatedAt = Instant.now()
        conversationRepository.save(conversation)

        return message.toDto()
    }

    @Transactional
    fun markRead(userId: UUID, conversationId: UUID): Int {
        val conversation = conversationRepository.findById(conversationId)
            .orElseThrow { NotFoundException("Conversation", conversationId) }
        requireParticipant(conversation, userId)
        return messageRepository.markAsRead(conversationId, userId)
    }

    fun getUnreadCount(userId: UUID): Long {
        return messageRepository.countUnread(userId)
    }

    // === Blocking ===

    @Transactional
    fun blockUser(blockerId: UUID, blockedId: UUID) {
        if (blockerId == blockedId) throw BadRequestException("Cannot block yourself")
        val blocker = userRepository.findById(blockerId)
            .orElseThrow { NotFoundException("User", blockerId) }
        val blocked = userRepository.findById(blockedId)
            .orElseThrow { NotFoundException("User", blockedId) }

        if (blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId) != null) {
            return // Already blocked
        }

        blockRepository.save(UserBlock(blocker = blocker, blocked = blocked))
    }

    @Transactional
    fun unblockUser(blockerId: UUID, blockedId: UUID) {
        val block = blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
            ?: throw NotFoundException("Block", blockedId)
        blockRepository.delete(block)
    }

    fun listBlocked(userId: UUID): List<BlockedUserDto> {
        return blockRepository.findByBlockerId(userId).map {
            BlockedUserDto(
                id = it.blocked.id,
                email = it.blocked.email,
                name = it.blocked.profile?.firstName,
                blockedAt = it.createdAt
            )
        }
    }

    // === Private helpers ===

    private fun resolveConversationType(initiator: User, target: User, initiatorRole: UserRole): ConversationType {
        return when {
            initiatorRole == UserRole.ADMIN -> when (target.role) {
                UserRole.PROVIDER -> ConversationType.PROVIDER_ADMIN
                UserRole.CLIENT -> ConversationType.ADMIN_CUSTOMER
                UserRole.ADMIN -> ConversationType.PROVIDER_ADMIN // admin-to-admin
            }
            initiatorRole == UserRole.PROVIDER && target.role == UserRole.ADMIN -> ConversationType.PROVIDER_ADMIN
            initiatorRole == UserRole.PROVIDER && target.role == UserRole.CLIENT -> ConversationType.CUSTOMER_PROVIDER
            initiatorRole == UserRole.CLIENT && target.role == UserRole.PROVIDER -> ConversationType.CUSTOMER_PROVIDER
            else -> throw BadRequestException("Cannot create conversation between these user types")
        }
    }

    private fun enforceConversationRules(
        initiator: User, target: User, initiatorRole: UserRole, type: ConversationType
    ) {
        when {
            // Admins can talk to anyone
            initiatorRole == UserRole.ADMIN -> return

            // Customers can create conversations with providers
            initiatorRole == UserRole.CLIENT && target.role == UserRole.PROVIDER -> return

            // Providers can create conversations with admins (with topic)
            initiatorRole == UserRole.PROVIDER && target.role == UserRole.ADMIN -> return

            // Providers can message customers only if previous contact exists
            initiatorRole == UserRole.PROVIDER && target.role == UserRole.CLIENT -> {
                val hasBookingHistory = bookingRepository
                    .findByProviderId(initiator.id)
                    .any { it.client.id == target.id }
                val hasConversationHistory = conversationRepository
                    .findByParticipants(initiator.id, target.id)
                    .isNotEmpty()

                if (!hasBookingHistory && !hasConversationHistory) {
                    throw ForbiddenException("Providers can only message customers they have had previous contact with")
                }
            }

            else -> throw ForbiddenException("Not allowed to create this conversation")
        }
    }

    private fun requireParticipant(conversation: Conversation, userId: UUID) {
        if (conversation.participant1.id != userId && conversation.participant2.id != userId) {
            throw ForbiddenException("You are not a participant in this conversation")
        }
    }

    private fun otherParticipantId(conversation: Conversation, userId: UUID): UUID {
        return if (conversation.participant1.id == userId) conversation.participant2.id
        else conversation.participant1.id
    }

    private fun Conversation.toDto(currentUserId: UUID): ConversationDto {
        val other = if (participant1.id == currentUserId) participant2 else participant1
        val lastMsg = messageRepository
            .findByConversationIdOrderByCreatedAtDesc(id, PageRequest.of(0, 1))
            .content.firstOrNull()

        return ConversationDto(
            id = id,
            otherParticipant = ParticipantDto(
                id = other.id,
                email = other.email,
                name = other.profile?.firstName,
                role = other.role.name
            ),
            conversationType = conversationType.name,
            topic = topic,
            lastMessage = lastMsg?.let {
                MessageSummaryDto(it.content, it.sender.id, it.createdAt, it.isRead)
            },
            updatedAt = updatedAt
        )
    }

    private fun Message.toDto() = MessageDto(
        id = id,
        senderId = sender.id,
        senderName = sender.profile?.firstName,
        content = content,
        isRead = isRead,
        createdAt = createdAt
    )
}
