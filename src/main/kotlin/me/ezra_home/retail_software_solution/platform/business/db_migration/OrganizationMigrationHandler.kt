package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.OrganizationLocationsMigration
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionService
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import me.ezra_home.retail_software_solution.util.enums.MigrationType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrganizationMigrationHandler(
  private val organizationCache: OrganizationCache,
  private val dbVersionService: DbVersionService,
  private val schemaMigrator: SchemaMigrator,
  private val migrationInitializer: MigrationInitializer,
  private val migrationStatusUpdater: MigrationStatusUpdater,
  private val locationBatchProcessor: LocationBatchProcessor,
  private val migrationFinalizer: MigrationFinalizer
) {
  fun migrateOrganizationAndLocations(
    organizationId: UUID,
    targetDbVersion: DbVersionEntity,
    locationIds: Set<UUID>
  ): OrganizationLocationsMigration {
    val organization = organizationCache.getAllOrganizations().find { it.id == organizationId }
      ?: throw RtsGenericException("Organization not found")

    val schemaName = organization.schemaName
      ?: throw RtsGenericException("Organization ${organization.name} has no schema name")

    val migrationType = if (locationIds.isEmpty()) MigrationType.ORG_ONLY else MigrationType.ORG_WITH_LOCATIONS
    val message = if (locationIds.isEmpty()) {
      "Migration in progress for organization only"
    } else {
      "Migration in progress for organization and locations"
    }

    val migration = migrationInitializer.createOrganizationMigration(
      organization = organization,
      targetDbVersion = targetDbVersion,
      migrationType = migrationType,
      message = message
    )

    return try {
      schemaMigrator.migrateOrganizationSchema(
        schemaName = schemaName,
        entityName = "Organization ${organization.name}",
        versionLabel = targetDbVersion.versionNumber,
        previousVersionLabel = dbVersionService.getVersionNumber(targetDbVersion.prevVersionId)
      )

      organization.currentDbVersionId = targetDbVersion.id
      organizationCache.upsertOrganization(organization)

      val locationResults = locationBatchProcessor.processLocations(
        organization = organization,
        targetDbVersion = targetDbVersion,
        parentMigrationId = migration.id!!,
        locationIds = locationIds
      )

      migrationFinalizer.finalizeOrganizationMigration(
        migration = migration,
        locationResults = locationResults,
        includesLocations = locationIds.isNotEmpty(),
        isRetry = false
      )

      OrganizationLocationsMigration(migration, locationResults.getAllResults())
    } catch (e: Exception) {
      migrationStatusUpdater.markFailure(migration, e)
      throw RtsGenericException("Organization migration failed: ${e.message}")
    }
  }
}

