package me.ezra_home.retail_software_solution.business.audit.auditmanager

import me.ezra_home.retail_software_solution.business.audit.AuditService
import me.ezra_home.retail_software_solution.model.enums.DatabaseAction
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class AuditBatchFlusher(
    private val auditBatchingContext: AuditBatchingContext,
    private val auditService: AuditService
) {

    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.SECONDS)
    fun flushAuditQueue() {
        doFlush()
    }

    private fun doFlush() {
        val mappedBatch = auditBatchingContext.getAuditBatchByAction()

        if (mappedBatch.isEmpty()) return

        mappedBatch[DatabaseAction.CREATE].let {
            if (!it.isNullOrEmpty()) {
                auditService.createAuditRecords(it)
            }
        }

        mappedBatch[DatabaseAction.UPDATE].let {
            if (!it.isNullOrEmpty()) {
                auditService.updateAuditRecords(it)
            }
        }

        mappedBatch[DatabaseAction.DELETE].let {
            if (!it.isNullOrEmpty()) {
                auditService.deleteAuditRecords(it)
            }
        }
    }

}
