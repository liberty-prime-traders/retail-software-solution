package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class SchemaMigrator(
  private val schemaExecutor: SchemaExecutor,
  private val migrationStatusUpdater: MigrationStatusUpdater
) {
  fun migrateOrganizationSchema(
    schemaName: String,
    versionLabel: String,
    entityName: String
  ) {
    try {
      schemaExecutor.executeOrganizationSchema(schemaName, versionLabel)
    } catch (e: Exception) {
      throw RtsGenericException("$entityName schema migration failed: ${e.message}")
    }
  }

  fun migrateLocationSchema(
    schemaName: String,
    versionLabel: String,
    migration: DbMigrationEntity,
    entityName: String
  ) {
    try {
      schemaExecutor.executeLocationSchema(schemaName, versionLabel)
      migrationStatusUpdater.markSuccess(migration, "Successfully migrated")
    } catch (e: Exception) {
      migrationStatusUpdater.markFailure(migration, e)
      throw RtsGenericException("$entityName migration failed: ${e.message}")
    }
  }
}

