package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.organizations.model.LocationEntity
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationRequestDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.MigrationHistoryResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionCache
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.business.SchemaCreator
import me.ezra_home.retail_software_solution.util.enums.MigrationStatus
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

@Service
class DbMigrationService(
    private val dbMigrationMapper: DbMigrationMapper,
    private val dbMigrationCache: DbMigrationCache,
    private val dbVersionCache: DbVersionCache,
    private val dbMigrationHistoryService: DbMigrationHistoryService,
    private val organizationCache: OrganizationCache,
    private val locationCache: LocationCache,
    @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_DATA_SOURCE)
    private val organizationDataSource: DataSource,
    @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_DATA_SOURCE)
    private val locationDataSource: DataSource,
) {
    @Value("\${spring.datasource.organization.changelog}")
    private lateinit var organizationChangeLog: String

    @Value("\${spring.datasource.location.changelog}")
    private lateinit var locationChangeLog: String

    @TransactionalOnPlatformSchema
    fun runSchemaMigration(dbMigrationRequestDto: DbMigrationRequestDto): DbMigrationResponseDto {
        val latestActivatedVersion = validateMigrationPreconditions(dbMigrationRequestDto)
        val dbMigrationEntity = when (dbMigrationRequestDto.schemaOwnerType) {
            SchemaOwnerType.ORGANIZATION -> migrateOrganizationAndLocations(
                schemaOwnerId = dbMigrationRequestDto.schemaOwnerId,
                targetDbVersion = latestActivatedVersion
            )

            SchemaOwnerType.LOCATION -> {
                migrateLocation(
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

    private fun migrateOrganizationAndLocations(
        schemaOwnerId: UUID,
        targetDbVersion: DbVersionEntity
    ): DbMigrationEntity {
        val organization = organizationCache.getAllOrganizations().find { it.id == schemaOwnerId }
            ?: throw RtsGenericException("Organization not found")
        val organizationSchemaName = organization.schemaName
            ?: throw RtsGenericException("Organization ${organization.name} has no schema name.")

        val orgMigration = DbMigrationEntity(
            dbVersionId = targetDbVersion.id!!,
            schemaOwnerId = schemaOwnerId,
            schemaOwnerType = SchemaOwnerType.ORGANIZATION,
            status = MigrationStatus.INITIATED,
            message = "Migration in progress for organization and its locations."
        )
        dbMigrationCache.upsertDbMigration(orgMigration)

        var orgMigrationMessage: String? = null
        val failedLocations = mutableListOf<String>()

        try {
            SchemaCreator.runMigration(
                schemaName = organizationSchemaName,
                dataSource = organizationDataSource,
                changeLog = organizationChangeLog,
                liquibaseLabel = targetDbVersion.versionNumber
            )

            // Migrate all locations belonging to this organization
            SessionContextProvider.initOrganization(organization)
            val locations = locationCache.getAllLocations()
            locations.forEach { location ->
                try {
                    migrateLocation(location, targetDbVersion)
                } catch (e: Exception) {
                    failedLocations.add("Location ${location.name} (ID: ${location.id}) - ${e.message}")
                }
            }

            if (failedLocations.isNotEmpty()) {
                orgMigration.status = MigrationStatus.PARTIAL
                orgMigrationMessage =
                    "Organization migrated, but some locations failed: ${failedLocations.joinToString("; ")}"
            } else {
                orgMigration.status = MigrationStatus.SUCCESS
                orgMigrationMessage = "Organization and all locations migrated successfully."
            }
        } catch (e: Exception) {
            orgMigration.status = MigrationStatus.FAILURE
            orgMigrationMessage = e.message?.take(100) ?: "Unknown error during organization migration."
            throw RtsGenericException("Organization migration failed: $orgMigrationMessage") // Abandon request
        } finally {
            orgMigration.endOn = OffsetDateTime.now()
            orgMigration.message = orgMigrationMessage?.take(100)
            dbMigrationCache.upsertDbMigration(orgMigration)
        }
        return orgMigration
    }

    private fun migrateLocation(
        location: LocationEntity,
        targetDbVersion: DbVersionEntity
    ): DbMigrationEntity {
        val locationSchemaName = location.schemaName
            ?: throw RtsGenericException("Location ${location.name} has no schema name.")

        val locationMigration = DbMigrationEntity(
            dbVersionId = targetDbVersion.id!!,
            schemaOwnerId = location.id!!,
            schemaOwnerType = SchemaOwnerType.LOCATION,
            status = MigrationStatus.INITIATED,
            message = "Locations migration in progress."
        )
        dbMigrationCache.upsertDbMigration(locationMigration)

        try {
            SchemaCreator.runMigration(
                schemaName = locationSchemaName,
                dataSource = locationDataSource,
                changeLog = locationChangeLog,
                liquibaseLabel = targetDbVersion.versionNumber
            )
            locationMigration.status = MigrationStatus.SUCCESS
            locationMigration.message = "Successfully migrated."
        } catch (e: Exception) {
            locationMigration.status = MigrationStatus.FAILURE
            locationMigration.message = e.message?.take(100) ?: "Unknown error"
            throw RtsGenericException("Location migration failed: ${locationMigration.message}")
        } finally {
            locationMigration.endOn = OffsetDateTime.now()
            dbMigrationCache.upsertDbMigration(locationMigration)
        }
        return locationMigration
    }

    fun getMigrationHistory(
        startDateTime: OffsetDateTime?,
        endDateTime: OffsetDateTime?
    ): List<MigrationHistoryResponseDto> {
        return dbMigrationHistoryService.getMigrationHistory(startDateTime, endDateTime)
    }
}
