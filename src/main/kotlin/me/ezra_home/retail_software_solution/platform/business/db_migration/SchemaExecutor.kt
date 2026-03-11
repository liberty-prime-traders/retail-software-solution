package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.util.business.SchemaCreator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class SchemaExecutor(
    @param:Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_DATA_SOURCE)
    private val organizationDataSource: DataSource,

    @param:Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_DATA_SOURCE)
    private val locationDataSource: DataSource,

    @param:Value("\${spring.datasource.organization.changelog}")
    private val organizationChangeLog: String,

    @param:Value("\${spring.datasource.location.changelog}")
    private val locationChangeLog: String
) {

  fun executeOrganizationSchema(schemaName: String, versionLabel: String, previousVersionLabel: String? = null) {
    SchemaCreator.runMigration(
        schemaName = schemaName,
        dataSource = organizationDataSource,
        changeLog = organizationChangeLog,
        targetVersion = versionLabel,
        previousVersion = previousVersionLabel
    )
  }

  fun executeLocationSchema(schemaName: String, versionLabel: String, previousVersionLabel: String? = null) {
    SchemaCreator.runMigration(
        schemaName = schemaName,
        dataSource = locationDataSource,
        changeLog = locationChangeLog,
        targetVersion = versionLabel,
        previousVersion = previousVersionLabel
    )
  }
}
