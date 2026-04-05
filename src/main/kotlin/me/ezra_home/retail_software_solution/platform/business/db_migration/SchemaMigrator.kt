package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.platform.business.db_migration.DbMigrationDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class SchemaMigrator(
  private val schemaExecutor: SchemaExecutor,
  private val migrationStatusUpdater: MigrationStatusUpdater
) {
  fun migrateOrganizationSchema(
    schemaName: String,
    entityName: String,
    versionLabel: String,
    previousVersionLabel: String? = null
  ) {
    try {
      schemaExecutor.executeOrganizationSchema(schemaName, versionLabel, previousVersionLabel)
    } catch (e: Exception) {
      throw RtsGenericException("$entityName schema migration failed: ${e.message}")
    }
  }

  fun migrateLocationSchema(
    schemaName: String,
    migration: DbMigrationDto,
    entityName: String,
    versionLabel: String,
    previousVersionLabel: String? = null
  ) {
    try {
      schemaExecutor.executeLocationSchema(schemaName, versionLabel, previousVersionLabel)
      migrationStatusUpdater.markSuccess(migration, "Successfully migrated")
    } catch (e: Exception) {
      migrationStatusUpdater.markFailure(migration, e)
      throw RtsGenericException("$entityName migration failed: ${e.message}")
    }
  }
}
