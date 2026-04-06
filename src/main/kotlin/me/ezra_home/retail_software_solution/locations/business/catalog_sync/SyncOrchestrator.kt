package me.ezra_home.retail_software_solution.locations.business.catalog_sync

import me.ezra_home.retail_software_solution.locations.business.catalog_sync.api.SyncLogFetcher
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.api.SyncMode
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.api.SyncStatus
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.strategy.SyncStrategyRegistry
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.sync_services.SyncServiceRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SyncOrchestrator(
  private val syncLogUpdater: SyncLogUpdater,
  private val strategyRegistry: SyncStrategyRegistry,
  private val serviceRegistry: SyncServiceRegistry,
  private val syncBatchProcessor: SyncBatchProcessor,
  private val syncLogFetcher: SyncLogFetcher,
) {

  private val logger = LoggerFactory.getLogger(SyncOrchestrator::class.java)

  fun process(syncLogEntity: SyncLogEntity) {
    val syncLogId = syncLogEntity.id ?: throw IllegalArgumentException("SyncLogEntity must have an ID to process")

    try {
      val resumableRevision = findResumableRevision(syncLogEntity)
      syncLogUpdater.markStarted(syncLogId)
      val tableName = syncLogEntity.tableName
      val syncMode = syncLogEntity.syncMode
      logger.info(
        "Starting {} sync for table {} (syncLogId: {}), resuming from revision: {}",
        syncMode, tableName, syncLogId, resumableRevision
      )

      val syncStrategy = strategyRegistry.getStrategy(syncMode)
      val syncService = serviceRegistry.getService(tableName)
      val initialCursor = syncStrategy.createInitialCursor(resumableRevision)

      val progressAccumulator = SyncProgressAccumulator(syncLogId)
      var wasCancelled = false

      syncBatchProcessor.execute(syncService, initialCursor, syncLogId) { batchResult ->
        if (batchResult.cancelled) {
          wasCancelled = true
        }
        val revision = syncStrategy.getCursorForStorage(batchResult.lastCursor)
        progressAccumulator.addBatch(batchResult, revision)
        syncLogUpdater.updateProgress(progressAccumulator.toUpdate())
      }

      if (wasCancelled) {
        syncLogUpdater.markComplete(syncLogId, SyncStatus.CANCELLED, "Sync cancelled by user")
        logger.info("Sync cancelled (syncLogId: {})", syncLogId)
      } else {
        syncLogUpdater.markComplete(syncLogId, SyncStatus.COMPLETED, null)
        logger.info("Sync completed successfully (syncLogId: {})", syncLogId)
      }

    } catch (e: Exception) {
      val errorMsg = e.message ?: e::class.simpleName ?: "Unknown error"
      logger.error("Sync failed (syncLogId: {}): {}", syncLogId, errorMsg, e)
      syncLogUpdater.markComplete(syncLogId, SyncStatus.FAILED, errorMsg)
    }
  }

  fun findResumableRevision(syncLogEntity: SyncLogEntity): Long? {
    return when (syncLogEntity.syncMode) {
      SyncMode.FULL -> null
      SyncMode.INCREMENTAL -> syncLogFetcher.findResumableRevision(syncLogEntity.tableName)
    }
  }
}
