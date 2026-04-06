package com.hps.persistence.messaging

import com.hps.domain.messaging.ConversationArchive
import com.hps.domain.messaging.ConversationArchiveId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConversationArchiveRepository : JpaRepository<ConversationArchive, ConversationArchiveId> {

    fun existsByIdUserIdAndIdConversationId(userId: UUID, conversationId: UUID): Boolean

    fun deleteByIdUserIdAndIdConversationId(userId: UUID, conversationId: UUID)

    fun findByIdUserId(userId: UUID): List<ConversationArchive>
}
