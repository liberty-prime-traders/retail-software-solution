package me.ezra_home.retail_software_solution.locations.business.catalog_sync

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.model.SyncLogEntity
import me.ezra_home.retail_software_solution.util.enums.SyncStatus
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID


@Service
@TransactionalOnLocationSchema
class SyncLogUpdater(
  private val syncLogRepository: SyncLogRepository,
  private val syncLogFetcher: SyncLogFetcher
) {

  fun createSyncLog(syncLog: SyncLogEntity): SyncLogEntity {
    return syncLogRepository.save(syncLog)
  }

  fun markStarted(syncLogId: UUID) {
    val syncLog = syncLogFetcher.findById(syncLogId)
    syncLog.startedAt = OffsetDateTime.now()
    syncLogRepository.save(syncLog)
  }

  fun updateProgress(update: SyncProgressUpdate) {
    val syncLog = syncLogFetcher.findById(update.syncLogId)
    syncLog.processedRecords = update.processed
    syncLog.skippedRecords = update.skipped
    syncLog.failedRecords = update.failed
    syncLog.lastProcessedRevision = update.lastRevision
    syncLogRepository.save(syncLog)
  }

  fun markComplete(syncLogId: UUID, status: SyncStatus, errorMessage: String?) {
    val syncLog = syncLogFetcher.findById(syncLogId)
    syncLog.status = status
    if (status != SyncStatus.CANCELLED) {
      syncLog.completedAt = OffsetDateTime.now()
    }
    syncLog.errorMessage = errorMessage
    syncLogRepository.save(syncLog)
  }

  fun requestCancellation(syncLogId: UUID) {
    val syncLog = syncLogFetcher.findById(syncLogId)
    if (syncLog.canceledAt == null) {
      syncLog.canceledAt = OffsetDateTime.now()
    }
    syncLog.status = SyncStatus.CANCELLATION_REQUESTED
    syncLogRepository.save(syncLog)
  }
}
