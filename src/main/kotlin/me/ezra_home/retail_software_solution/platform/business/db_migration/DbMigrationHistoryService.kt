package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.organizations.model.LocationEntity
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.LocationMigrationResponse
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.MigrationHistoryResponse
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionRepository
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationService
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnPlatformSchema(readOnly = true)
class DbMigrationHistoryService(
    private val dbMigrationRepository: DbMigrationRepository,
    private val dbVersionRepository: DbVersionRepository,
    private val organizationService: OrganizationService,
) {
    fun getMigrationHistory(
        startDateTime: OffsetDateTime?,
        endDateTime: OffsetDateTime?
    ): List<MigrationHistoryResponse> {
        val allMigrations = if (startDateTime != null && endDateTime != null) {
            dbMigrationRepository.findByStartOnBetweenOrderByStartOnDesc(
                startDateTime,
                endDateTime
            )
        } else {
            dbMigrationRepository.findAll().sortedByDescending { it.startOn }
        }

        val dbVersionsMap = dbVersionRepository.findAll().associateBy { it.id }
        val organizationsWithLocations = organizationService.getAllOrganizationsWithLocations()

        fun findLocationWithOrgId(locationId: UUID): Pair<LocationEntity, UUID>? {
            for (organizationWithLocations in organizationsWithLocations) {
                val location = organizationWithLocations.locations.find { it.id == locationId }
                if (location != null) {
                    return Pair(location, organizationWithLocations.organization.id!!)
                }
            }
            return null
        }

        val organizationMigrationsMap = mutableMapOf<UUID, MutableList<LocationMigrationResponse>>()
        val topLevelOrgMigrations = mutableListOf<MigrationHistoryResponse>()

        for (migration in allMigrations) {
            val versionNumber = dbVersionsMap[migration.dbVersionId]?.versionNumber ?: "UNKNOWN"

            when (migration.schemaOwnerType) {
                SchemaOwnerType.ORGANIZATION -> {
                    val organizationName =
                        organizationsWithLocations.find { it.organization.id == migration.schemaOwnerId }?.organization?.name
                    topLevelOrgMigrations.add(
                        MigrationHistoryResponse(
                            organizationId = migration.schemaOwnerId,
                            organizationName = organizationName,
                            versionNumber = versionNumber,
                            startDate = migration.startOn,
                            endDate = migration.endOn,
                            migrationResult = migration.migrationResult,
                            message = migration.message,
                            locations = mutableListOf()
                        )
                    )
                }

                SchemaOwnerType.LOCATION -> {
                    val result = findLocationWithOrgId(migration.schemaOwnerId)
                    val locationName = result?.first?.name
                    val locationOrganizationId = result?.second

                    organizationMigrationsMap.getOrPut(locationOrganizationId!!) { mutableListOf() }
                        .add(
                            LocationMigrationResponse(
                                locationId = migration.schemaOwnerId,
                                locationName = locationName,
                                versionNumber = versionNumber,
                                startDate = migration.startOn,
                                endDate = migration.endOn,
                                migrationResult = migration.migrationResult,
                                message = migration.message
                            )
                        )
                }
            }
        }

        // Populate locations for organization migrations
        return topLevelOrgMigrations.map { topLevelOrgMigration ->
            val associatedLocations = organizationMigrationsMap[topLevelOrgMigration.organizationId]
            topLevelOrgMigration.copy(locations = associatedLocations?.sortedByDescending { it.startDate }
                ?: emptyList())
        }.sortedByDescending { it.startDate }
    }
}
