package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import org.springframework.stereotype.Component

@Component
class MigrationFinalizer(
  private val migrationStatusUpdater: MigrationStatusUpdater
) {
  fun finalizeOrganizationMigration(
    migration: DbMigrationEntity,
    locationResults: LocationMigrationResults,
    includesLocations: Boolean,
    isRetry: Boolean
  ) {
    if (locationResults.failed.isNotEmpty()) {
      val prefix = when {
        isRetry -> "Successfully retried some locations, but others failed"
        includesLocations -> "Organization migrated, but some locations failed"
        else -> "Organization migrated successfully"
      }
      migrationStatusUpdater.markPartial(migration, "$prefix: ${locationResults.failed.joinToString("; ")}")
    } else {
      val message = when {
        isRetry -> "All specified locations successfully retried"
        includesLocations -> "Organization and all its specified locations migrated successfully"
        else -> "Organization migrated successfully"
      }
      migrationStatusUpdater.markSuccess(migration, message)
    }
  }
}

