package me.ezra_home.retail_software_solution.business.audit.auditmanager

import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.model.entity.BaseEntity
import me.ezra_home.retail_software_solution.model.enums.DatabaseAction
import me.ezra_home.retail_software_solution.rest.session.SessionContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

@Component
class AuditBatchingContext {

    private val auditQueue = ConcurrentLinkedDeque<TransientAuditEntity>()
    private val stashedAuditQueue = ConcurrentLinkedDeque<TransientAuditEntity>()
    private val batchFetchingInProgress = AtomicBoolean(false)

    @Async
    fun logEntityActivity(entity: BaseEntity, action: DatabaseAction, sessionContext: SessionContext) {
        val auditRecord = generateAuditRecord(entity, action, sessionContext)
        if (batchFetchingInProgress.get()) {
            stashedAuditQueue.addLast(auditRecord)
        } else {
            auditQueue.addLast(auditRecord)
        }
    }

    private fun generateAuditRecord(entity: BaseEntity, action: DatabaseAction, sessionContext: SessionContext): TransientAuditEntity {
        val entityName = getTableName(entity.javaClass)
        val entityId = entity.id ?: throw RuntimeException("Attempted to add a record without an id to the audit table")
        val offsetDateTime = OffsetDateTime.now()
        return TransientAuditEntity(action,
            offsetDateTime,
            sessionContext.oktaId!!,
            entityName,
            entityId
        )
    }

    private fun getTableName(entityClass: Class<BaseEntity>): String =
        entityClass.getAnnotation(Table::class.java)?.name ?: entityClass.simpleName ?: "UnknownEntity"


    fun getAuditBatchByAction(): Map<DatabaseAction, Collection<TransientAuditEntity>> {
        if (batchFetchingInProgress.get()) return mapOf()

        val batch = mutableListOf<TransientAuditEntity>()
        batchFetchingInProgress.set(true)
        while (auditQueue.isNotEmpty()) {
            auditQueue.pollLast().let { queueElement ->
                if (batch.none { batchElement -> batchElement.matchesWith(queueElement) }) {
                    batch.add(queueElement)
                }
            }
        }
        while (stashedAuditQueue.isNotEmpty()) {
            auditQueue.addLast(stashedAuditQueue.pollFirst())
        }
        batchFetchingInProgress.set(false)

        return batch.groupBy { it.action}
    }
}
