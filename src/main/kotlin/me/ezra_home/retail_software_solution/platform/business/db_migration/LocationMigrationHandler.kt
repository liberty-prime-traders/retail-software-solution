package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.organizations.model.LocationEntity
import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
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
class LocationMigrationHandler(
    private val dbMigrationCache: DbMigrationCache,
    @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_DATA_SOURCE)
    private val locationDataSource: DataSource,
) {
    @Value("\${spring.datasource.location.changelog}")
    private lateinit var locationChangeLog: String

    fun migrateLocation(
        location: LocationEntity,
        targetDbVersion: DbVersionEntity,
        migrationType: MigrationType? = null,
        migrationParentId: UUID? = null
    ): DbMigrationEntity {
        val locationSchemaName = location.schemaName
            ?: throw RtsGenericException("Location ${location.name} has no schema name.")

        val locationMigration = DbMigrationEntity(
            dbVersionId = targetDbVersion.id!!,
            schemaOwnerId = location.id!!,
            schemaOwnerType = SchemaOwnerType.LOCATION,
            status = MigrationStatus.INITIATED,
            type = migrationType,
            message = "Locations migration in progress.",
            migrationParentId = migrationParentId
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
            locationMigration.message = "Successfully migrated"
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
}