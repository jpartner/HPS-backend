package com.hps.persistence.messaging

import com.hps.domain.messaging.MessageReport
import com.hps.domain.messaging.ReportStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MessageReportRepository : JpaRepository<MessageReport, UUID> {

    fun findByTenantId(tenantId: UUID): List<MessageReport>

    fun findByTenantIdAndStatus(tenantId: UUID, status: ReportStatus): List<MessageReport>

    fun existsByMessageIdAndReporterId(messageId: UUID, reporterId: UUID): Boolean
}
