package me.ezra_home.retail_software_solution.locations.business.catalog_sync.public

import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncBatchProcessor
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncLogEntity
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncLogMapper
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncLogUpdater
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncOrchestrator
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncValidator
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.strategy.SyncStrategyRegistry
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.sync_services.SyncServiceRegistry
import me.ezra_home.retail_software_solution.util.async.AsyncExecutor
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Service

@Service
class SyncInitiator(
  private val validator: SyncValidator,
  private val syncLogUpdater: SyncLogUpdater,
  private val strategyRegistry: SyncStrategyRegistry,
  private val syncServiceRegistry: SyncServiceRegistry,
  private val syncOrchestrator: SyncOrchestrator,
  private val asyncExecutor: AsyncExecutor,
  private val syncLogMapper: SyncLogMapper
) {

  fun initiate(tableName: TableName, syncMode: SyncMode, initiateType: SyncInitiateType): SyncLogResponseDto {
    validator.validateCanSync(tableName)
    val syncStrategy = strategyRegistry.getStrategy(syncMode)
    val syncService = syncServiceRegistry.getService(tableName)

    val totalRecords = if (syncStrategy.shouldCalculateTotal()) { syncService.countAllRecords() } else { null }

    val syncLog = SyncLogEntity(
      tableName = tableName,
      syncMode = syncMode,
      initiateType = initiateType,
      totalRecords = totalRecords,
      status = SyncStatus.IN_PROGRESS
    )
    val savedLog = syncLogUpdater.createSyncLog(syncLog)

    asyncExecutor.execute {
      syncOrchestrator.process(savedLog)
    }

    return syncLogMapper.toDto(savedLog)
  }
}
