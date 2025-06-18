package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.organizations.model.LocationEntity
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationRequestDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.MigrationHistoryResponse
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionService
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationService
import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.business.SchemaCreator
import me.ezra_home.retail_software_solution.util.enums.MigrationResult
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
    private val dbVersionService: DbVersionService,
    private val dbMigrationRepository: DbMigrationRepository,
    private val dbMigrationHistoryService: DbMigrationHistoryService,
    private val organizationCache: OrganizationCache,
    private val organizationService: OrganizationService,
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
    fun runSchemaMigration(dbMigrationRequestDto: DbMigrationRequestDto): DbMigrationEntity? {
        val latestActivatedVersion = dbVersionService.getLatestActivatedDbVersion()
            ?: throw RtsGenericException("No active DB version found to migrate to.")

        val previousVersionId = latestActivatedVersion.prevVersionId
        if (previousVersionId != null) {
            val previousMigration =
                dbMigrationRepository.findTopBySchemaOwnerIdAndSchemaOwnerTypeAndDbVersionIdOrderByStartOnDesc(
                    dbMigrationRequestDto.schemaOwnerId,
                    dbMigrationRequestDto.schemaOwnerType,
                    previousVersionId
                )

            if (previousMigration.isEmpty().not()) {
                if (previousMigration[0].migrationResult != MigrationResult.SUCCESS) {
                    throw RtsGenericException("Previous version migration (ID: $previousVersionId) for owner ${dbMigrationRequestDto.schemaOwnerId} of type ${dbMigrationRequestDto.schemaOwnerType} was not successful.")
                }
            }
        }

        return when (dbMigrationRequestDto.schemaOwnerType) {
            SchemaOwnerType.ORGANIZATION -> migrateOrganizationAndLocations(
                dbMigrationRequestDto.schemaOwnerId,
                latestActivatedVersion
            )

            SchemaOwnerType.LOCATION -> migrateSingleLocation(
                dbMigrationRequestDto.schemaOwnerId,
                latestActivatedVersion
            )
        }
    }

    private fun migrateOrganizationAndLocations(
        schemaOwnerId: UUID,
        targetDbVersion: DbVersionEntity
    ): DbMigrationEntity {
        val organization = organizationCache.getAllOrganizations().find { it.id == schemaOwnerId }
            ?: throw RtsGenericException("Organization not found")
        val organizationSchemaName = organization.schemaName
            ?: throw RtsGenericException("Organization ${organization.name} has no schema name.")

        val orgMigration = dbMigrationRepository.save(
            DbMigrationEntity(
                dbVersionId = targetDbVersion.id!!,
                schemaOwnerId = schemaOwnerId,
                schemaOwnerType = SchemaOwnerType.ORGANIZATION,
                migrationResult = MigrationResult.PARTIAL,
                message = "Migration in progress for organization and its locations."
            )
        )

        var orgMigrationSuccess = true
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
                val locationSchemaName = location.schemaName
                    ?: run {
                        failedLocations.add("Location ${location.name} (ID: ${location.id}) - No schema name")
                        return@forEach
                    }

                var locMigrationEntity: DbMigrationEntity? = null
                try {
                    locMigrationEntity = dbMigrationRepository.save(
                        DbMigrationEntity(
                            dbVersionId = targetDbVersion.id!!,
                            schemaOwnerId = location.id!!,
                            schemaOwnerType = SchemaOwnerType.LOCATION,
                            migrationResult = MigrationResult.PARTIAL,
                            message = "Location migration in progress."
                        )
                    )
                    SchemaCreator.runMigration(
                        schemaName = locationSchemaName,
                        dataSource = locationDataSource,
                        changeLog = locationChangeLog,
                        liquibaseLabel = targetDbVersion.versionNumber
                    )
                    locMigrationEntity.migrationResult = MigrationResult.SUCCESS
                    locMigrationEntity.message = "Successfully migrated."
                } catch (e: Exception) {
                    failedLocations.add("Location ${location.name} (ID: ${location.id}) - ${e.message}")
                    if (locMigrationEntity != null) {
                        locMigrationEntity.migrationResult = MigrationResult.FAILURE
                        locMigrationEntity.message = e.message?.take(100) ?: "Unknown error"
                    }
                } finally {
                    locMigrationEntity?.apply {
                        endOn = OffsetDateTime.now()
                        dbMigrationRepository.save(this)
                    }
                }
            }

            if (failedLocations.isNotEmpty()) {
                orgMigrationSuccess = false
                orgMigrationMessage =
                    "Organization migrated, but some locations failed: ${failedLocations.joinToString("; ")}"
            } else {
                orgMigrationMessage = "Organization and all locations migrated successfully."
            }

        } catch (e: Exception) {
            orgMigrationSuccess = false
            orgMigrationMessage = e.message?.take(255) ?: "Unknown error during organization migration."
            throw RtsGenericException("Organization migration failed: ${orgMigrationMessage}") // Abandon request
        } finally {
            orgMigration.endOn = OffsetDateTime.now()
            orgMigration.migrationResult = if (orgMigrationSuccess) MigrationResult.SUCCESS else MigrationResult.FAILURE
            orgMigration.message = orgMigrationMessage?.take(100)
            dbMigrationRepository.save(orgMigration)
        }
        return orgMigration
    }

    private fun migrateSingleLocation(locationId: UUID, targetDbVersion: DbVersionEntity): DbMigrationEntity? {
        val organizationsWithLocations = organizationService.getAllOrganizationsWithLocations()
        var location: LocationEntity? = null

        for (organizationWithLocations in organizationsWithLocations) {
            location = organizationWithLocations.locations.find { it.id == locationId }
        }

        if (location == null) {
            throw RtsGenericException("Location does not exist")
        }

        val locationSchemaName = location.schemaName
            ?: throw RtsGenericException("Location ${location.name} has no schema name.")

        val locationMigration = targetDbVersion.id?.let {
            DbMigrationEntity(
                dbVersionId = it,
                schemaOwnerId = locationId,
                schemaOwnerType = SchemaOwnerType.LOCATION,
                migrationResult = MigrationResult.PARTIAL,
                message = "Locations migration in progress."
            )
        }?.let {
            dbMigrationRepository.save(
                it
            )
        }

        try {
            SchemaCreator.runMigration(
                schemaName = locationSchemaName,
                dataSource = locationDataSource,
                changeLog = locationChangeLog,
                liquibaseLabel = targetDbVersion.versionNumber
            )
            if (locationMigration != null) {
                locationMigration.migrationResult = MigrationResult.SUCCESS
                locationMigration.message = "Successfully migrated."
            }
        } catch (e: Exception) {
            if (locationMigration != null) {
                locationMigration.migrationResult = MigrationResult.FAILURE
                locationMigration.message = e.message?.take(255) ?: "Unknown error"
                throw RtsGenericException("Location migration failed: ${locationMigration.message}")
            }
        } finally {
            if (locationMigration != null) {
                locationMigration.endOn = OffsetDateTime.now()
                dbMigrationRepository.save(locationMigration)
            }
        }
        return locationMigration
    }

    fun getMigrationHistory(
        startDateTime: OffsetDateTime?,
        endDateTime: OffsetDateTime?
    ): List<MigrationHistoryResponse> {
        return dbMigrationHistoryService.getMigrationHistory(startDateTime, endDateTime)
    }
}