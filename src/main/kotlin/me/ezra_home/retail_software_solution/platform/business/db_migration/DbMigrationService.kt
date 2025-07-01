package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.organizations.model.LocationEntity
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationRequestDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.MigrationHistoryResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionCache
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.enums.MigrationStatus
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class DbMigrationService(
    private val dbMigrationMapper: DbMigrationMapper,
    private val dbMigrationCache: DbMigrationCache,
    private val dbVersionCache: DbVersionCache,
    private val dbMigrationHistoryService: DbMigrationHistoryService,
    private val organizationCache: OrganizationCache,
    private val locationCache: LocationCache,
    private val organizationMigrationHandler: OrganizationMigrationHandler,
    private val locationMigrationHandler: LocationMigrationHandler
) {
    @TransactionalOnPlatformSchema
    fun runSchemaMigration(dbMigrationRequestDto: DbMigrationRequestDto): DbMigrationResponseDto {
        val latestActivatedVersion = validateMigrationPreconditions(dbMigrationRequestDto)
        val dbMigrationEntity = when (dbMigrationRequestDto.schemaOwnerType) {
            SchemaOwnerType.ORGANIZATION -> organizationMigrationHandler.migrateOrganizationAndLocations(
                schemaOwnerId = dbMigrationRequestDto.schemaOwnerId,
                targetDbVersion = latestActivatedVersion
            )

            SchemaOwnerType.LOCATION -> {
                locationMigrationHandler.migrateLocation(
                    location = validateLocationExistence(dbMigrationRequestDto),
                    targetDbVersion = latestActivatedVersion
                )
            }
        }
        return dbMigrationMapper.toResponseDto(dbMigrationEntity)
    }

    private fun validateMigrationPreconditions(dbMigrationRequestDto: DbMigrationRequestDto): DbVersionEntity {
        val latestActivatedVersion = dbVersionCache.getLatestActivatedDbVersion()
            ?: throw RtsGenericException("No active DB version found to migrate to.")
        latestActivatedVersion.prevVersionId?.let { previousVersionId ->
            val previousMigration =
                dbMigrationCache.getTopBySchemaOwnerIdAndSchemaOwnerTypeAndDbVersionIdOrderByStartOnDesc(
                    dbMigrationRequestDto.schemaOwnerId,
                    dbMigrationRequestDto.schemaOwnerType,
                    previousVersionId
                )
            previousMigration?.run {
                if (previousMigration.status != MigrationStatus.SUCCESS) {
                    throw RtsGenericException("Previous migration attempt (Version: $previousVersionId) was not successful.")
                }
            }
        }
        return latestActivatedVersion
    }

    private fun validateLocationExistence(dbMigrationRequestDto: DbMigrationRequestDto): LocationEntity {
        val organizationId = dbMigrationRequestDto.organizationId
            ?: throw RtsGenericException("organizationId is required")
        val organization = organizationCache.getAllOrganizations().find { it.id == organizationId }
            ?: throw RtsGenericException("Organization not found")
        SessionContextProvider.initOrganization(organization)
        val location = locationCache.getAllLocations().find { it.id == dbMigrationRequestDto.schemaOwnerId }
            ?: throw RtsGenericException("Location does not exist")
        return location
    }

    fun getMigrationHistory(
        startDateTime: OffsetDateTime?,
        endDateTime: OffsetDateTime?
    ): List<MigrationHistoryResponseDto> {
        return dbMigrationHistoryService.getMigrationHistory(startDateTime, endDateTime)
    }
}
