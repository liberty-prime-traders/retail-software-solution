package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
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
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

@Component
class OrganizationMigrationHandler(
    private val dbMigrationCache: DbMigrationCache,
    private val organizationCache: OrganizationCache,
    private val locationCache: LocationCache,
    private val locationMigrationHandler: LocationMigrationHandler, // Inject the new handler
    @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_DATA_SOURCE)
    private val organizationDataSource: DataSource,
) {
    @Value("\${spring.datasource.organization.changelog}")
    private lateinit var organizationChangeLog: String

    fun migrateOrganizationAndLocations(
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
                    locationMigrationHandler.migrateLocation(location, targetDbVersion)
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
            throw RtsGenericException("Organization migration failed: $orgMigrationMessage")
        } finally {
            orgMigration.endOn = OffsetDateTime.now()
            orgMigration.message = orgMigrationMessage?.take(100)
            dbMigrationCache.upsertDbMigration(orgMigration)
        }
        return orgMigration
    }
}