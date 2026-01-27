package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import me.ezra_home.retail_software_solution.platform.model.OrganizationEntity
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class LocationBatchProcessor(
  private val locationCache: LocationCache,
  private val schemaMigrator: SchemaMigrator,
  private val migrationInitializer: MigrationInitializer
) {
  fun processLocations(
    organization: OrganizationEntity,
    targetDbVersion: DbVersionEntity,
    parentMigrationId: UUID,
    locationIds: Set<UUID>
  ): LocationMigrationResults {
    if (locationIds.isEmpty()) {
      return LocationMigrationResults.empty()
    }

    SessionContextProvider.initOrganization(organization)
    val locations = locationCache.getAllLocations().filter { it.id in locationIds }

    if (locations.isEmpty()) {
      throw RtsGenericException("None of the specified locations were found for migration")
    }

    val successful = mutableListOf<DbMigrationEntity>()
    val failed = mutableListOf<String>()

    locations.forEach { location ->
      val schemaName = location.schemaName
      if (schemaName == null) {
        failed.add("Location ${location.name} (ID: ${location.id}) - No schema name")
        return@forEach
      }

      val locationMigration = migrationInitializer.createLocationMigration(
        location = location,
        targetDbVersion = targetDbVersion,
        parentMigrationId = parentMigrationId
      )

      try {
        schemaMigrator.migrateLocationSchema(
          schemaName = schemaName,
          versionLabel = targetDbVersion.versionNumber,
          migration = locationMigration,
          entityName = "Location ${location.name}"
        )
        successful.add(locationMigration)
      } catch (e: Exception) {
        failed.add("Location ${location.name} (ID: ${location.id}) - ${e.message}")
      }
    }

    return LocationMigrationResults(successful, failed)
  }
}

