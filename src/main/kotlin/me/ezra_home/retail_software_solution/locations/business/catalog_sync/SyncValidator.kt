package me.ezra_home.retail_software_solution.locations.business.catalog_sync

import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Component


@Component
internal class SyncValidator(private val syncLogFetcher: SyncLogFetcher) {

  fun validateCanSync(tableName: TableName) {
    require(tableName.schemaLevel == SchemaLevel.ORGANIZATION) {
      "Only organization schema tables can be synced to location schema. " +
        "Table $tableName is at ${tableName.schemaLevel} level."
    }

    if (syncLogFetcher.existsInProgressSync(tableName)) {
      val existingSync = syncLogFetcher.findInProgressSync(tableName)
      throw IllegalStateException(
        "Sync already in progress for table $tableName (syncLogId: ${existingSync?.id}). " +
          "Wait for it to complete or cancel it before starting a new sync."
      )
    }
  }
}
