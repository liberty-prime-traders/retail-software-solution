package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.OrganizationLocationsMigration
import me.ezra_home.retail_software_solution.platform.business.db_version.dto.DbVersionDto
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MigrationRetryHandler(
  private val organizationCache: OrganizationCache,
  private val migrationInitializer: MigrationInitializer,
  private val migrationStatusUpdater: MigrationStatusUpdater,
  private val locationBatchProcessor: LocationBatchProcessor,
  private val migrationFinalizer: MigrationFinalizer,
  private val dbMigrationCache: DbMigrationCache
) {
  fun retryLocationMigrations(
    originalMigration: DbMigrationDto,
    targetDbVersion: DbVersionDto,
    locationIds: Set<UUID>
  ): OrganizationLocationsMigration {
    val organization = organizationCache.getAllOrganizations().find { it.id == originalMigration.schemaOwnerId }
      ?: throw RtsGenericException("Organization not found")

    val retryMigration = migrationInitializer.createRetryMigration(originalMigration, targetDbVersion)

    return try {
      val eligibleLocationIds = filterEligibleLocations(originalMigration.getNullSafeId(), locationIds)

      if (eligibleLocationIds.isEmpty()) {
        migrationStatusUpdater.markIgnored(
          retryMigration,
          "No failed locations to retry or provided locations were already successful"
        )
        return OrganizationLocationsMigration(retryMigration, emptyList())
      }

      val locationResults = locationBatchProcessor.processLocations(
        organization = organization,
        targetDbVersion = targetDbVersion,
        parentMigrationId = retryMigration.getNullSafeId(),
        locationIds = eligibleLocationIds
      )

      migrationFinalizer.finalizeOrganizationMigration(
        migration = retryMigration,
        locationResults = locationResults,
        includesLocations = true,
        isRetry = true
      )

      val consolidatedResults = consolidateResults(originalMigration.getNullSafeId(), locationResults.successful)

      OrganizationLocationsMigration(retryMigration, consolidatedResults)
    } catch (e: Exception) {
      migrationStatusUpdater.markFailure(retryMigration, e)
      throw RtsGenericException("Locations retry failed: ${e.message}")
    }
  }

  private fun filterEligibleLocations(originalMigrationId: UUID, requestedIds: Set<UUID>): Set<UUID> {
    return requestedIds.filterTo(mutableSetOf()) { locationId ->
      hasFailedMigration(originalMigrationId, locationId)
    }
  }

  private fun consolidateResults(
    originalMigrationId: UUID,
    newResults: List<DbMigrationDto>
  ): List<DbMigrationDto> {
    val previousAttempts = dbMigrationCache.getDbLocationMigrationsByMigrationsParentId(originalMigrationId)
    val newResultsMap = newResults.associateBy { it.schemaOwnerId }

    return previousAttempts.map { previous ->
      newResultsMap[previous.schemaOwnerId] ?: previous
    }
  }

  private fun hasFailedMigration(parentMigrationId: UUID, locationId: UUID): Boolean {
    return dbMigrationCache.getLatestFailedLocationMigrationForOrgParent(parentMigrationId, locationId) != null
  }
}
