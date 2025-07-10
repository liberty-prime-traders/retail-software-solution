package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationRequestDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationRetryRequestDto
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionCache
import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import me.ezra_home.retail_software_solution.util.enums.MigrationStatus
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
class DbMigrationService(
    private val dbMigrationMapper: DbMigrationMapper,
    private val dbMigrationCache: DbMigrationCache,
    private val dbVersionCache: DbVersionCache,
    private val organizationMigrationHandler: OrganizationMigrationHandler,
) {
    @TransactionalOnPlatformSchema
    fun runSchemaMigration(dbMigrationRequestDto: DbMigrationRequestDto): DbMigrationResponseDto {
        val latestActivatedVersion = validateMigrationPreconditions(dbMigrationRequestDto)
        val organizationLocationsMigration = organizationMigrationHandler.migrateOrganizationAndLocations(
            schemaOwnerId = dbMigrationRequestDto.schemaOwnerId,
            targetDbVersion = latestActivatedVersion,
            locationIdsToMigrate = dbMigrationRequestDto.locationIdsToMigrate
        )
        return dbMigrationMapper.toResponseDto(organizationLocationsMigration.organizationMigration).apply {
            locations =
                organizationLocationsMigration.locationMigrations.map { dbMigrationMapper.toResponseDto(it) }
        }
    }

    private fun validateMigrationPreconditions(dbMigrationRequestDto: DbMigrationRequestDto): DbVersionEntity {
        if (dbMigrationRequestDto.locationIdsToMigrate.isEmpty()) {
            throw RtsGenericException("At least one location must be specified for migration")
        }
        val latestActivatedVersion = dbVersionCache.getLatestActivatedDbVersion()
            ?: throw RtsGenericException("No active DB version found to migrate to")
        latestActivatedVersion.let { previousVersion ->
            val previousMigration =
                previousVersion.prevVersionId?.let {
                    dbMigrationCache.getTopBySchemaOwnerIdAndSchemaOwnerTypeAndDbVersionIdOrderByStartOnDesc(
                        dbMigrationRequestDto.schemaOwnerId, SchemaOwnerType.ORGANIZATION, it
                    )
                }
            previousMigration?.run {
                if (previousMigration.status != MigrationStatus.SUCCESS) {
                    throw RtsGenericException("Previous migration attempt (Version: ${previousVersion.versionNumber}) was not successful.")
                }
            }
        }
        return latestActivatedVersion
    }


    @TransactionalOnPlatformSchema
    fun retryFailedLocationMigrations(dbMigrationRetryRequestDto: DbMigrationRetryRequestDto): DbMigrationResponseDto {
        val originalOrgMigration = validateThenReturnOriginalMigration(dbMigrationRetryRequestDto)
        val targetDbVersion = dbVersionCache.getAllDbVersions().find { it.id == originalOrgMigration.dbVersionId }
            ?: throw RtsGenericException("Target DB version not found for original migration")
        val organizationLocationsMigration = organizationMigrationHandler.retryLocationsMigration(
            originalOrgMigration = originalOrgMigration,
            targetDbVersion = targetDbVersion,
            locationIdsToRetry = dbMigrationRetryRequestDto.locationIdsToMigrate
        )
        return dbMigrationMapper.toResponseDto(organizationLocationsMigration.organizationMigration).apply {
            locations = organizationLocationsMigration.locationMigrations.map { dbMigrationMapper.toResponseDto(it) }
        }
    }

    private fun validateThenReturnOriginalMigration(dbMigrationRetryRequestDto: DbMigrationRetryRequestDto): DbMigrationEntity {
        val originalOrgMigration =
            dbMigrationCache.getAllDbMigrations().find { it.id == dbMigrationRetryRequestDto.orgMigrationId }
                ?: throw RtsGenericException("Original DB migration not found")
        if (originalOrgMigration.schemaOwnerType != SchemaOwnerType.ORGANIZATION) {
            throw RtsGenericException("Only organization-level migrations can be retried")
        }
        if (originalOrgMigration.status != MigrationStatus.PARTIAL) {
            throw RtsGenericException("Only partially completed migrations can be retried")
        }
        return originalOrgMigration
    }

}
