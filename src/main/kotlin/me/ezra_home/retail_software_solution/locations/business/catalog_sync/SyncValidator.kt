package me.ezra_home.retail_software_solution.locations.business.catalog_sync

import me.ezra_home.retail_software_solution.locations.business.catalog_sync.api.SyncLogFetcher
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Component


@Component
class SyncValidator(private val syncLogFetcher: SyncLogFetcher) {

  fun validateCanSync(tableName: TableName) {
    if (tableName.schemaLevel != SchemaLevel.ORGANIZATION) {
      throw RtsGenericException(
        "Only organization schema tables can be synced to location schema. " +
          "Table $tableName is at ${tableName.schemaLevel} level."
      )
    }

    if (syncLogFetcher.existsInProgressSync(tableName)) {
      val existingSync = syncLogFetcher.findInProgressSync(tableName)
      throw RtsGenericException(
        "Sync already in progress for table $tableName (syncLogId: ${existingSync?.id}). " +
          "Wait for it to complete or cancel it before starting a new sync."
      )
    }
  }
}
