package me.ezra_home.retail_software_solution.locations.business.catalog_sync

import java.util.UUID

internal class SyncProgressAccumulator(val syncLogId: UUID) {

  private var totalProcessed = 0
  private var totalSkipped = 0
  private var totalFailed = 0
  private var lastRevision: Long? = null

  fun addBatch(batchResult: SyncBatchProcessor.BatchResult, revision: Long?) {
    totalProcessed += batchResult.processed
    totalSkipped += batchResult.skipped
    totalFailed += batchResult.failed
    lastRevision = revision
  }

  fun toUpdate(): SyncProgressUpdate {
    return SyncProgressUpdate(
      syncLogId = syncLogId,
      processed = totalProcessed,
      skipped = totalSkipped,
      failed = totalFailed,
      lastRevision = lastRevision
    )
  }

}
