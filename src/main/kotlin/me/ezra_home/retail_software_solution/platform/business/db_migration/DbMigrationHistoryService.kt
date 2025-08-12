package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.organizations.model.LocationEntity
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.LocationMigrationResponse
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.MigrationHistoryResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionCache
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationService
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnPlatformSchema(readOnly = true)
class DbMigrationHistoryService(
    private val dbMigrationCache: DbMigrationCache,
    private val dbVersionCache: DbVersionCache,
    private val organizationService: OrganizationService,
) {
    fun getMigrationHistory(
        startDateTime: OffsetDateTime,
        endDateTime: OffsetDateTime
    ): List<MigrationHistoryResponseDto> {
        val allMigrations = dbMigrationCache.getAllDbMigrationsFilteredByDate(startDateTime, endDateTime)

        val dbVersionsMap = dbVersionCache.getAllDbVersions().associateBy { it.id }
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
        val topLevelOrgMigrations = mutableListOf<MigrationHistoryResponseDto>()

        for (migration in allMigrations) {
            val versionNumber = dbVersionsMap[migration.dbVersionId]?.versionNumber ?: "UNKNOWN"

            when (migration.schemaOwnerType) {
                SchemaOwnerType.ORGANIZATION -> {
                    val organizationName =
                        organizationsWithLocations.find { it.organization.id == migration.schemaOwnerId }?.organization?.name
                    topLevelOrgMigrations.add(
                        MigrationHistoryResponseDto(
                            id = migration.id,
                            organizationId = migration.schemaOwnerId,
                            organizationName = organizationName,
                            versionNumber = versionNumber,
                            startDate = migration.startOn,
                            endDate = migration.endOn,
                            status = migration.status,
                            message = migration.message,
                            type = migration.type,
                            migrationParentId = migration.migrationParentId,
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
                                status = migration.status,
                                message = migration.message,
                                migrationParentId = migration.migrationParentId,
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
