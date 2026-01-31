package me.ezra_home.retail_software_solution.locations.business.sync

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.model.SyncLogEntity
import me.ezra_home.retail_software_solution.locations.business.sync.dto.SyncLogResponseDto
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SyncLogFetcher(
    private val syncLogRepository: SyncLogRepository,
    private val syncLogMapper: SyncLogMapper
)  {

    fun findById(syncLogId: UUID): SyncLogEntity {
        return syncLogRepository.findById(syncLogId).orElseThrow {
            IllegalArgumentException("SyncLog not found: $syncLogId")
        }
    }

    fun findInProgressSync(tableName: TableName): SyncLogEntity? {
        return syncLogRepository.findInProgressSync(tableName)
    }

    fun existsInProgressSync(tableName: TableName): Boolean {
        return syncLogRepository.existsInProgressSync(tableName)
    }

    fun getSyncLogById(syncLogId: UUID): SyncLogResponseDto? {
        return try {
            syncLogMapper.toDto(findById(syncLogId))
        } catch(_: IllegalArgumentException) {
            null
        }

    }

    fun findResumableRevision(tableName: TableName): Long? {
        val failedRevision = syncLogRepository.findLastFailedSyncRevision(tableName)
        if (failedRevision != null) {
            return failedRevision
        }
        val cancelledRevision = syncLogRepository.findLastCancelledSyncRevision(tableName)
        if (cancelledRevision != null) {
            return cancelledRevision
        }
        return syncLogRepository.findLastSuccessfulSyncRevision(tableName)
    }

    fun isSyncCancelled(syncLogId: UUID): Boolean {
        return syncLogRepository.isCancelled(syncLogId)
    }
}
