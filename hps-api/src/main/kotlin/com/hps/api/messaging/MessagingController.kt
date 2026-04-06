package com.hps.api.messaging

import com.hps.api.auth.role
import com.hps.api.auth.userId
import com.hps.common.tenant.TenantContext
import com.hps.domain.messaging.ReportStatus
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class MessagingController(
    private val messagingService: MessagingService
) {
    @GetMapping("/conversations")
    fun listConversations(
        @RequestParam(defaultValue = "false") archived: Boolean,
        auth: Authentication
    ): List<ConversationDto> =
        messagingService.listConversations(auth.userId(), archived)

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    fun createConversation(
        @Valid @RequestBody request: CreateConversationRequest,
        auth: Authentication
    ): ConversationDto =
        messagingService.createConversation(auth.userId(), auth.role(), request)

    @GetMapping("/conversations/{id}/messages")
    fun getMessages(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        auth: Authentication
    ): List<MessageDto> =
        messagingService.getMessages(auth.userId(), id, page, size)

    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    fun sendMessage(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SendMessageRequest,
        auth: Authentication
    ): MessageDto =
        messagingService.sendMessage(auth.userId(), id, request)

    @PostMapping("/conversations/{id}/read")
    fun markRead(@PathVariable id: UUID, auth: Authentication): Map<String, Int> {
        val count = messagingService.markRead(auth.userId(), id)
        return mapOf("markedRead" to count)
    }

    @GetMapping("/messages/unread-count")
    fun unreadCount(auth: Authentication): Map<String, Long> =
        mapOf("unread" to messagingService.getUnreadCount(auth.userId()))

    // === Archive ===

    @PostMapping("/conversations/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun archiveConversation(@PathVariable id: UUID, auth: Authentication) {
        messagingService.archiveConversation(auth.userId(), id)
    }

    @DeleteMapping("/conversations/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unarchiveConversation(@PathVariable id: UUID, auth: Authentication) {
        messagingService.unarchiveConversation(auth.userId(), id)
    }

    // === Reporting ===

    @PostMapping("/messages/{id}/report")
    @ResponseStatus(HttpStatus.CREATED)
    fun reportMessage(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ReportMessageRequest,
        auth: Authentication
    ): MessageReportDto =
        messagingService.reportMessage(auth.userId(), id, request.reason)

    // === Blocking ===

    @PostMapping("/users/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    fun blockUser(@Valid @RequestBody request: BlockUserRequest, auth: Authentication) {
        messagingService.blockUser(auth.userId(), request.userId)
    }

    @DeleteMapping("/users/blocks/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unblockUser(@PathVariable userId: UUID, auth: Authentication) {
        messagingService.unblockUser(auth.userId(), userId)
    }

    @GetMapping("/users/blocks")
    fun listBlocked(auth: Authentication): List<BlockedUserDto> =
        messagingService.listBlocked(auth.userId())
}

// --- Admin report management ---

@RestController
@RequestMapping("/api/v1/admin/message-reports")
class AdminMessageReportController(
    private val messagingService: MessagingService
) {
    @GetMapping
    fun listReports(
        @RequestParam(required = false) status: String?
    ): List<MessageReportDto> {
        val tenantId = TenantContext.require()
        val reportStatus = status?.let { ReportStatus.valueOf(it) }
        return messagingService.listReports(tenantId, reportStatus)
    }

    @PutMapping("/{id}")
    fun reviewReport(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ReviewReportRequest,
        auth: Authentication
    ) {
        messagingService.reviewReport(auth.userId(), id, request)
    }
}
