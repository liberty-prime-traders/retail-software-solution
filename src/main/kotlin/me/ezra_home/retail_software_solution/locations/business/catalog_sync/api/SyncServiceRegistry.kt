package me.ezra_home.retail_software_solution.locations.business.catalog_sync.api

import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Component

@Component
class SyncServiceRegistry(
  private val syncServices: List<SyncService<*>>
) {

  fun getService(tableName: TableName): SyncService<*> {
    return syncServices.firstOrNull { it.supports(tableName) }
      ?: throw IllegalArgumentException("No sync service registered for table: $tableName")
  }
}
