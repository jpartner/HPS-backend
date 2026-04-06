package com.hps.api.messaging

import com.hps.common.tenant.TenantContext
import com.hps.common.exception.BadRequestException
import com.hps.common.exception.ForbiddenException
import com.hps.common.exception.NotFoundException
import com.hps.domain.messaging.*
import com.hps.domain.user.User
import com.hps.domain.user.UserRole
import com.hps.persistence.booking.BookingRepository
import com.hps.persistence.messaging.*
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
    private val archiveRepository: ConversationArchiveRepository,
    private val reportRepository: MessageReportRepository,
    private val userRepository: UserRepository,
    private val bookingRepository: BookingRepository
) {
    fun listConversations(userId: UUID, archived: Boolean = false): List<ConversationDto> {
        val conversations = conversationRepository.findByParticipant(userId)
        val archivedIds = archiveRepository.findByIdUserId(userId)
            .map { it.id.conversationId }.toSet()

        return conversations
            .filter { if (archived) archivedIds.contains(it.id) else !archivedIds.contains(it.id) }
            .map { it.toDto(userId, archivedIds.contains(it.id)) }
    }

    fun getMessages(userId: UUID, conversationId: UUID, page: Int, size: Int): List<MessageDto> {
        val conversation = conversationRepository.findById(conversationId)
            .orElseThrow { NotFoundException("Conversation", conversationId) }
        requireParticipant(conversation, userId)

        return messageRepository
            .findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(page, size))
            .content.map { it.toDto() }
    }

    @Transactional
    fun createConversation(userId: UUID, userRole: UserRole, request: CreateConversationRequest): ConversationDto {
        val initiator = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User", userId) }

        // Resolve target by handle or ID
        val target = resolveTarget(request, initiator)

        if (userId == target.id) {
            throw BadRequestException("Cannot create conversation with yourself")
        }

        if (blockRepository.isBlocked(userId, target.id)) {
            throw ForbiddenException("Cannot communicate with this user")
        }

        val conversationType = resolveConversationType(initiator, target, userRole)
        enforceConversationRules(initiator, target, userRole, conversationType)

        // Always create a new conversation thread.
        // Replies to existing threads go via POST /conversations/{id}/messages.
        val tenantId = TenantContext.require()
        val conversation = conversationRepository.save(
            Conversation(
                tenantId = tenantId,
                participant1 = initiator,
                participant2 = target,
                conversationType = conversationType,
                topic = request.topic
            )
        )

        messageRepository.save(Message(conversation = conversation, sender = initiator, content = request.initialMessage))
        conversation.updatedAt = Instant.now()
        conversationRepository.save(conversation)

        return conversation.toDto(userId, false)
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
        val message = messageRepository.save(Message(conversation = conversation, sender = sender, content = request.content))

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

    fun getUnreadCount(userId: UUID): Long = messageRepository.countUnread(userId)

    // === Archive ===

    @Transactional
    fun archiveConversation(userId: UUID, conversationId: UUID) {
        if (!conversationRepository.existsById(conversationId)) {
            throw NotFoundException("Conversation", conversationId)
        }
        if (archiveRepository.existsByIdUserIdAndIdConversationId(userId, conversationId)) return
        archiveRepository.save(ConversationArchive(ConversationArchiveId(userId, conversationId)))
    }

    @Transactional
    fun unarchiveConversation(userId: UUID, conversationId: UUID) {
        archiveRepository.deleteByIdUserIdAndIdConversationId(userId, conversationId)
    }

    // === Reporting ===

    @Transactional
    fun reportMessage(userId: UUID, messageId: UUID, reason: String): MessageReportDto {
        val message = messageRepository.findById(messageId)
            .orElseThrow { NotFoundException("Message", messageId) }

        val conversation = message.conversation
        requireParticipant(conversation, userId)

        if (message.sender.id == userId) {
            throw BadRequestException("Cannot report your own message")
        }
        if (reportRepository.existsByMessageIdAndReporterId(messageId, userId)) {
            throw BadRequestException("You have already reported this message")
        }

        val reporter = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User", userId) }
        val tenantId = TenantContext.require()

        val report = reportRepository.save(
            MessageReport(message = message, reporter = reporter, reason = reason, tenantId = tenantId)
        )
        return report.toDto()
    }

    fun listReports(tenantId: UUID, status: ReportStatus?): List<MessageReportDto> {
        val reports = if (status != null) {
            reportRepository.findByTenantIdAndStatus(tenantId, status)
        } else {
            reportRepository.findByTenantId(tenantId)
        }
        return reports.map { it.toDto() }
    }

    @Transactional
    fun reviewReport(adminId: UUID, reportId: UUID, request: ReviewReportRequest) {
        val report = reportRepository.findById(reportId)
            .orElseThrow { NotFoundException("MessageReport", reportId) }
        val admin = userRepository.findById(adminId)
            .orElseThrow { NotFoundException("User", adminId) }

        report.status = ReportStatus.valueOf(request.status)
        report.reviewedBy = admin
        report.reviewedAt = Instant.now()
        report.adminNotes = request.adminNotes
        reportRepository.save(report)
    }

    // === Blocking ===

    @Transactional
    fun blockUser(blockerId: UUID, blockedId: UUID) {
        if (blockerId == blockedId) throw BadRequestException("Cannot block yourself")
        val blocker = userRepository.findById(blockerId).orElseThrow { NotFoundException("User", blockerId) }
        val blocked = userRepository.findById(blockedId).orElseThrow { NotFoundException("User", blockedId) }
        if (blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId) != null) return
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
                handle = it.blocked.handle,
                email = it.blocked.email,
                name = it.blocked.profile?.firstName,
                blockedAt = it.createdAt
            )
        }
    }

    // === Private helpers ===

    private fun resolveTarget(request: CreateConversationRequest, initiator: User): User {
        if (request.participantHandle != null) {
            val handle = request.participantHandle.lowercase().removePrefix("@")
            if (handle == "admin") {
                // Find first active admin for this tenant
                val tenantId = TenantContext.require()
                return userRepository.findByTenantIdAndRole(tenantId, UserRole.ADMIN)
                    .firstOrNull { it.isActive }
                    ?: throw BadRequestException("No admin available for this tenant")
            }
            val tenantId = TenantContext.require()
            return userRepository.findByHandleAndTenantId(handle, tenantId)
                ?: throw NotFoundException("User", "@$handle")
        }
        if (request.participantId != null) {
            return userRepository.findById(request.participantId)
                .orElseThrow { NotFoundException("User", request.participantId) }
        }
        throw BadRequestException("Either participantId or participantHandle must be provided")
    }

    private fun resolveConversationType(initiator: User, target: User, initiatorRole: UserRole): ConversationType {
        return when {
            initiatorRole == UserRole.ADMIN || initiatorRole == UserRole.SUPER_ADMIN -> when (target.role) {
                UserRole.PROVIDER -> ConversationType.PROVIDER_ADMIN
                UserRole.CLIENT -> ConversationType.ADMIN_CUSTOMER
                UserRole.ADMIN, UserRole.SUPER_ADMIN -> ConversationType.PROVIDER_ADMIN
            }
            initiatorRole == UserRole.PROVIDER && target.role == UserRole.ADMIN -> ConversationType.PROVIDER_ADMIN
            initiatorRole == UserRole.PROVIDER && target.role == UserRole.CLIENT -> ConversationType.CUSTOMER_PROVIDER
            initiatorRole == UserRole.CLIENT && target.role == UserRole.PROVIDER -> ConversationType.CUSTOMER_PROVIDER
            else -> throw BadRequestException("Cannot create conversation between these user types")
        }
    }

    private fun enforceConversationRules(initiator: User, target: User, initiatorRole: UserRole, type: ConversationType) {
        when {
            initiatorRole == UserRole.ADMIN || initiatorRole == UserRole.SUPER_ADMIN -> return
            initiatorRole == UserRole.CLIENT && target.role == UserRole.PROVIDER -> return
            initiatorRole == UserRole.PROVIDER && target.role == UserRole.ADMIN -> return
            initiatorRole == UserRole.PROVIDER && target.role == UserRole.CLIENT -> {
                val hasHistory = bookingRepository.findByProviderId(initiator.id).any { it.client.id == target.id }
                    || conversationRepository.findByParticipants(initiator.id, target.id).isNotEmpty()
                if (!hasHistory) throw ForbiddenException("Providers can only message customers they have had previous contact with")
            }
            else -> throw ForbiddenException("Not allowed to create this conversation")
        }
    }

    private fun requireParticipant(conversation: Conversation, userId: UUID) {
        if (conversation.participant1.id != userId && conversation.participant2.id != userId) {
            throw ForbiddenException("You are not a participant in this conversation")
        }
    }

    private fun otherParticipantId(conversation: Conversation, userId: UUID): UUID =
        if (conversation.participant1.id == userId) conversation.participant2.id else conversation.participant1.id

    private fun Conversation.toDto(currentUserId: UUID, isArchived: Boolean): ConversationDto {
        val other = if (participant1.id == currentUserId) participant2 else participant1
        val lastMsg = messageRepository
            .findByConversationIdOrderByCreatedAtDesc(id, PageRequest.of(0, 1))
            .content.firstOrNull()

        return ConversationDto(
            id = id,
            otherParticipant = ParticipantDto(
                id = other.id, handle = other.handle, email = other.email,
                name = other.profile?.firstName, role = other.role.name
            ),
            conversationType = conversationType.name,
            topic = topic,
            lastMessage = lastMsg?.let { MessageSummaryDto(it.content, it.sender.id, it.createdAt, it.isRead) },
            isArchived = isArchived,
            updatedAt = updatedAt
        )
    }

    private fun Message.toDto() = MessageDto(
        id = id, senderId = sender.id, senderHandle = sender.handle,
        senderName = sender.profile?.firstName, content = content,
        isRead = isRead, createdAt = createdAt
    )

    private fun MessageReport.toDto() = MessageReportDto(
        id = id, messageId = message.id, messageContent = message.content,
        reporterHandle = reporter.handle, reporterEmail = reporter.email,
        reason = reason, status = status.name,
        reviewedBy = reviewedBy?.email, reviewedAt = reviewedAt,
        adminNotes = adminNotes, createdAt = createdAt
    )
}
