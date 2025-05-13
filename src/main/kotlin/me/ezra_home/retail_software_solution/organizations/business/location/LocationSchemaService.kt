package me.ezra_home.retail_software_solution.organizations.business.location

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.util.business.SchemaCreator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class LocationSchemaService(
    @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_DATA_SOURCE)
    private val dataSource: DataSource
) {

    @Value("\${spring.datasource.location.changelog}")
    private lateinit var changeLog: String

    fun createSchema(schemaName: String) {
        SchemaCreator.createSchema(schemaName, dataSource, changeLog)
    }

    fun dropSchema(schemaName: String) {
        SchemaCreator.dropSchema(schemaName, dataSource)
    }

}
