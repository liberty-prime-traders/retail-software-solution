package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.LocationMigrationResponse
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.OrganizationMigrationResponseDto
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
    fun getMigrationHistory(startDateTime: OffsetDateTime, endDateTime: OffsetDateTime): List<OrganizationMigrationResponseDto> {
        val organizationsWithLocations = organizationService.getAllOrganizationsWithLocations()

        val orgIdToName: Map<UUID, String?> = buildMap {
            organizationsWithLocations.forEach {
                put(it.organization.getNullSafeId(), it.organization.name)
            }
        }

        val locIdToName: Map<UUID, String?> = buildMap {
            organizationsWithLocations.forEach {
                org -> org.locations.forEach {
                    put(it.getNullSafeId(), it.name)
                }
            }
        }

        val locationsByParentId = mutableMapOf<UUID, MutableList<LocationMigrationResponse>>()
        val topLevelOrgMigrations = mutableListOf<OrganizationMigrationResponseDto>()

        val dbVersionsMap = dbVersionCache.getAllDbVersions().associateBy { it.id }
        val allMigrations = dbMigrationCache.getAllDbMigrationsFilteredByDate(startDateTime, endDateTime)
        for (migration in allMigrations) {
            val versionNumber = dbVersionsMap[migration.dbVersionId]?.versionNumber

            when (migration.schemaOwnerType) {
                SchemaOwnerType.ORGANIZATION -> {
                    val organizationId = migration.schemaOwnerId
                    val organizationName = orgIdToName[organizationId]
                    topLevelOrgMigrations.add(
                        OrganizationMigrationResponseDto(
                            id = migration.getNullSafeId(),
                            organizationName = organizationName!!,
                            versionNumber = versionNumber!!,
                            startOn = migration.startOn,
                            endOn = migration.endOn,
                            status = migration.status,
                            message = migration.message,
                            locations = mutableListOf()
                        )
                    )
                }

                SchemaOwnerType.LOCATION -> {
                    val locationName = locIdToName[migration.schemaOwnerId] ?: continue
                    val parentId = migration.migrationParentId ?: throw IllegalStateException("Parent ID cannot be null for location migration.")
                    val locResponse = LocationMigrationResponse(
                        locationId = migration.schemaOwnerId,
                        locationName = locationName,
                        versionNumber = versionNumber!!,
                        startOn = migration.startOn,
                        endOn = migration.endOn,
                        status = migration.status,
                        message = migration.message,
                    )

                    locationsByParentId.getOrPut(parentId) { mutableListOf() }.add(locResponse)
                }
            }
        }

        return topLevelOrgMigrations.map {
            it.apply { this.locations = locationsByParentId[this.id] }
        }
    }
}
