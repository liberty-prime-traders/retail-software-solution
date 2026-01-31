package me.ezra_home.retail_software_solution.locations.business.sync.sync_services

import me.ezra_home.retail_software_solution.locations.business.sync.SyncCursor
import me.ezra_home.retail_software_solution.util.model.TableName

interface SyncService<T> {

  fun getTableName(): TableName

  fun supports(tableName: TableName): Boolean = getTableName() == tableName

  fun countAllRecords(): Int

  fun fetchBatch(cursor: SyncCursor?, batchSize: Int): List<T>

  fun createLocationRecord(record: T): Boolean

  fun extractCursor(record: T): SyncCursor
}
