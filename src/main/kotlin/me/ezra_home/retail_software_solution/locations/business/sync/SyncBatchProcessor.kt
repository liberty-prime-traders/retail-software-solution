package me.ezra_home.retail_software_solution.locations.business.sync

import me.ezra_home.retail_software_solution.locations.business.sync.sync_services.SyncService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SyncBatchProcessor(private val syncLogFetcher: SyncLogFetcher) {

  private val logger = LoggerFactory.getLogger(SyncBatchProcessor::class.java)
  private val batchSize = 10

  data class BatchResult(
    val processed: Int,
    val skipped: Int,
    val failed: Int,
    val lastCursor: SyncCursor?,
    val cancelled: Boolean = false
  )

  fun <T> execute(
    service: SyncService<T>,
    initialCursor: SyncCursor?,
    syncLogId: UUID,
    onBatchComplete: (BatchResult) -> Unit
  ) {
    var currentCursor = initialCursor
    var hasMore = true

    while (hasMore) {
      if (syncLogFetcher.isSyncCancelled(syncLogId)) {
        logger.info("Sync cancellation detected, stopping batch processing")
        onBatchComplete(BatchResult(0, 0, 0, currentCursor, true))
        return
      }

      val batch = service.fetchBatch(currentCursor, batchSize)

      if (batch.isEmpty()) {
        hasMore = false
      } else {
        val result: BatchResult = processBatch(service, batch)
        onBatchComplete(result)

        currentCursor = result.lastCursor
        logger.debug(
          "Processed batch: {} created, {} skipped, {} failed",
          result.processed, result.skipped, result.failed
        )
      }
    }
  }

  private fun <T> processBatch(service: SyncService<T>, batch: List<T>): BatchResult {
    var processed = 0
    var skipped = 0
    var failed = 0

    for (record in batch) {
      try {
        val created = service.createLocationRecord(record)
        if (created) {
          processed++
        } else {
          skipped++
        }
      } catch (e: Exception) {
        logger.error("Failed to sync record: ${e.message}", e)
        failed++
      }
    }

    val lastCursor = if (batch.isNotEmpty()) {
      service.extractCursor(batch.last())
    } else null

    return BatchResult(processed, skipped, failed, lastCursor)
  }
}
