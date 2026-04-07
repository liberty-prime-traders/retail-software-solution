package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.platform.business.db_version.api.DbVersionDto
import me.ezra_home.retail_software_solution.platform.business.db_version.api.DbVersionService
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class LocationBatchProcessor(
  private val locationService: LocationService,
  private val schemaMigrator: SchemaMigrator,
  private val dbVersionService: DbVersionService,
  private val migrationInitializer: MigrationInitializer
) {
  fun processLocations(
    organization: OrganizationDto,
    targetDbVersion: DbVersionDto,
    parentMigrationId: UUID,
    locationIds: Set<UUID>
  ): LocationMigrationResults {
    if (locationIds.isEmpty()) {
      return LocationMigrationResults.empty()
    }

    SessionContextProvider.initOrganization(organization)
    val locations = locationService.getAllLocationDtos().filter { it.id in locationIds }

    if (locations.isEmpty()) {
      throw RtsGenericException("None of the specified locations were found for migration")
    }

    val successful = mutableListOf<DbMigrationDto>()
    val failed = mutableListOf<DbMigrationDto>()

    locations.forEach { location ->
      val schemaName = location.schemaName ?: return@forEach

      val locationMigration = migrationInitializer.createLocationMigration(
        location = location,
        targetDbVersion = targetDbVersion,
        parentMigrationId = parentMigrationId
      )

      try {
        schemaMigrator.migrateLocationSchema(
          schemaName = schemaName,
          migration = locationMigration,
          entityName = "Location ${location.name}",
          versionLabel = targetDbVersion.versionNumber,
          previousVersionLabel = dbVersionService.getVersionNumber(targetDbVersion.prevVersionId)
        )
        successful.add(locationMigration)
      } catch (_: Exception) {
        failed.add(locationMigration)
      }
    }

    return LocationMigrationResults(successful, failed)
  }
}
