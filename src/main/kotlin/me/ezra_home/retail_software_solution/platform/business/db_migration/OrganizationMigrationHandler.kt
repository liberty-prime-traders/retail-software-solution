package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.OrganizationLocationsMigration
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import me.ezra_home.retail_software_solution.platform.model.OrganizationEntity
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.business.SchemaCreator
import me.ezra_home.retail_software_solution.util.enums.MigrationStatus
import me.ezra_home.retail_software_solution.util.enums.MigrationType
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

@Component
class OrganizationMigrationHandler(
    private val dbMigrationCache: DbMigrationCache,
    private val organizationCache: OrganizationCache,
    private val locationCache: LocationCache,
    private val locationMigrationHandler: LocationMigrationHandler,
    @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_DATA_SOURCE)
    private val organizationDataSource: DataSource,
) {
    @Value("\${spring.datasource.organization.changelog}")
    private lateinit var organizationChangeLog: String

    fun migrateOrganizationAndLocations(
        schemaOwnerId: UUID,
        targetDbVersion: DbVersionEntity,
        locationIdsToMigrate: Set<UUID>
    ): OrganizationLocationsMigration {
        val organization = organizationCache.getAllOrganizations().find { it.id == schemaOwnerId }
            ?: throw RtsGenericException("Organization not found")
        val organizationSchemaName = organization.schemaName
            ?: throw RtsGenericException("Organization ${organization.name} has no schema name.")

        val orgMigration = DbMigrationEntity(
            dbVersionId = targetDbVersion.id!!,
            schemaOwnerId = schemaOwnerId,
            schemaOwnerType = SchemaOwnerType.ORGANIZATION,
            status = MigrationStatus.INITIATED,
            type = MigrationType.ORG_WITH_LOCATIONS,
            message = "Migration in progress for organization and its locations."
        )
        dbMigrationCache.upsertDbMigration(orgMigration)

        var locationMigrationResults: List<DbMigrationEntity>
        var failedLocationsMessages: List<String>

        try {
            SchemaCreator.runMigration(
                schemaName = organizationSchemaName,
                dataSource = organizationDataSource,
                changeLog = organizationChangeLog,
                liquibaseLabel = targetDbVersion.versionNumber
            )

            val (successfulLocMigrations, failedLocMessages) = executeLocationMigrations(
                organization,
                targetDbVersion,
                orgMigration.id!!,
                locationIdsToMigrate
            )
            locationMigrationResults = successfulLocMigrations
            failedLocationsMessages = failedLocMessages

            updateAndPersistParentMigrationStatus(
                parentMigration = orgMigration,
                failedLocationsMessages = failedLocationsMessages,
                overallSuccessMessage = "Organization and all its specified locations migrated successfully",
                overallPartialMessage = "Organization migrated, but some locations failed"
            )

        } catch (e: Exception) {
            orgMigration.endOn = OffsetDateTime.now()
            orgMigration.status = MigrationStatus.FAILURE
            orgMigration.message = e.message?.take(100) ?: "Unknown error during organization migration"
            dbMigrationCache.upsertDbMigration(orgMigration)
            throw RtsGenericException("Organization migration failed: ${orgMigration.message}")
        }

        return OrganizationLocationsMigration(
            organizationMigration = orgMigration,
            locationMigrations = locationMigrationResults,
        )
    }

    fun retryLocationsMigration(
        originalOrgMigration: DbMigrationEntity,
        targetDbVersion: DbVersionEntity,
        locationIdsToRetry: Set<UUID>
    ): OrganizationLocationsMigration {
        val organization = organizationCache.getAllOrganizations().find { it.id == originalOrgMigration.schemaOwnerId }
            ?: throw RtsGenericException("Organization not found for retry")
        val newOrgMigration = DbMigrationEntity(
            dbVersionId = targetDbVersion.id!!,
            schemaOwnerId = originalOrgMigration.schemaOwnerId,
            schemaOwnerType = SchemaOwnerType.ORGANIZATION,
            status = MigrationStatus.INITIATED,
            message = "Retry of failed locations from migration ${originalOrgMigration.id}",
            migrationParentId = originalOrgMigration.id!!,
            type = MigrationType.LOCATIONS_ONLY
        )
        dbMigrationCache.upsertDbMigration(newOrgMigration)

        var failedLocationsMessages: List<String>
        var locationMigrationResults: List<DbMigrationEntity>

        try {
            val actualLocationIdsToRetry = mutableSetOf<UUID>()
            locationIdsToRetry.forEach { locationId ->
                val latestLocationMigration =
                    dbMigrationCache.getLatestFailedLocationMigrationForOrgParent(originalOrgMigration.id!!, locationId)
                latestLocationMigration?.let {
                    actualLocationIdsToRetry.add(locationId)
                }
            }

            if (actualLocationIdsToRetry.isEmpty()) {
                newOrgMigration.status = MigrationStatus.IGNORED
                newOrgMigration.message = "No failed locations to retry or provided locations were already successful"
                newOrgMigration.endOn = OffsetDateTime.now()
                dbMigrationCache.upsertDbMigration(newOrgMigration)
                return OrganizationLocationsMigration(
                    organizationMigration = newOrgMigration,
                    locationMigrations = listOf(),
                )
            }

            val (successfulLocMigrations, failedLocMessages) = executeLocationMigrations(
                organization,
                targetDbVersion,
                newOrgMigration.id!!,
                actualLocationIdsToRetry
            )
            locationMigrationResults = successfulLocMigrations
            failedLocationsMessages = failedLocMessages

            updateAndPersistParentMigrationStatus(
                parentMigration = newOrgMigration,
                failedLocationsMessages = failedLocationsMessages,
                overallSuccessMessage = "All specified locations successfully retried",
                overallPartialMessage = "Successfully retried some locations, but others failed"
            )

        } catch (e: Exception) {
            newOrgMigration.endOn = OffsetDateTime.now()
            newOrgMigration.status = MigrationStatus.FAILURE
            newOrgMigration.message = e.message?.take(100) ?: "Unknown error during locations migration retry"
            dbMigrationCache.upsertDbMigration(newOrgMigration)
            throw RtsGenericException("Locations migrations retry failed: ${newOrgMigration.message}")
        }

        val parentLocationsMigrations = dbMigrationCache.getDbLocationMigrationsByMigrationsParentId(
            originalOrgMigration.id!!
        )

        val updatedLocationMigrations = parentLocationsMigrations.map { parentLocationsMigration ->
            locationMigrationResults.firstOrNull { locationMigrationResult ->
                locationMigrationResult.schemaOwnerId == parentLocationsMigration.schemaOwnerId
            } ?: parentLocationsMigration
        }

        return OrganizationLocationsMigration(
            organizationMigration = newOrgMigration,
            locationMigrations = updatedLocationMigrations,
        )
    }

    private fun executeLocationMigrations(
        organization: OrganizationEntity,
        targetDbVersion: DbVersionEntity,
        parentMigrationId: UUID,
        locationIdsToProcess: Set<UUID>
    ): Pair<List<DbMigrationEntity>, List<String>> {
        val locationMigrationResults = mutableListOf<DbMigrationEntity>()
        val failedLocationsMessages = mutableListOf<String>()

        SessionContextProvider.initOrganization(organization)
        val locationsToMigrate = locationCache.getAllLocations()
            .filter { location -> locationIdsToProcess.contains(location.id) }
        if (locationsToMigrate.isEmpty() && locationIdsToProcess.isNotEmpty()) {
            throw RtsGenericException("None of the specified locations were found for migration.")
        }

        locationsToMigrate.forEach { location ->
            try {
                val locationMigrationResult = locationMigrationHandler.migrateLocation(
                    location = location,
                    targetDbVersion = targetDbVersion,
                    migrationParentId = parentMigrationId
                )
                locationMigrationResults.add(locationMigrationResult)
            } catch (e: Exception) {
                failedLocationsMessages.add("Location ${location.name} (ID: ${location.id}) - ${e.message}")
            }
        }
        return Pair(locationMigrationResults, failedLocationsMessages)
    }

    private fun updateAndPersistParentMigrationStatus(
        parentMigration: DbMigrationEntity,
        failedLocationsMessages: List<String>,
        overallSuccessMessage: String,
        overallPartialMessage: String
    ) {
        if (failedLocationsMessages.isNotEmpty()) {
            parentMigration.status = MigrationStatus.PARTIAL
            parentMigration.message = "$overallPartialMessage: ${failedLocationsMessages.joinToString("; ")}"
        } else {
            parentMigration.status = MigrationStatus.SUCCESS
            parentMigration.message = overallSuccessMessage
        }
        parentMigration.endOn = OffsetDateTime.now()
        parentMigration.message = parentMigration.message?.take(100)
        dbMigrationCache.upsertDbMigration(parentMigration)
    }
}