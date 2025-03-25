package me.ezra_home.retail_software_solution.configuration.datasource

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.sql.Connection
import javax.sql.DataSource

@Component(DataSourceBeanNames.LOCATION_SCHEMA_CONNECTION_PROVIDER)
class LocationSchemaConnectionProvider(
    @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_DATA_SOURCE) private val dataSource: DataSource
): MultiTenantConnectionProvider<String> {

    override fun getAnyConnection(): Connection = dataSource.connection

    override fun getConnection(locationSchema: String): Connection {
        val connection = getAnyConnection()
        try {
            connection.createStatement().use { statement ->
                statement.execute("SET SCHEMA '$locationSchema'") // PostgreSQL syntax
            }
        } catch (e: Exception) {
            connection.close()
            throw RuntimeException("Failed to set schema to '$locationSchema'", e)
        }
        return connection
    }

    override fun releaseConnection(locationSchema: String, connection: Connection) {
        connection.close()
    }

    override fun releaseAnyConnection(connection: Connection) {
        connection.close()
    }

    override fun supportsAggressiveRelease(): Boolean = true
    override fun isUnwrappableAs(unwrapType: Class<*>): Boolean = false
    override fun <T: Any?> unwrap(type: Class<T>): T? = null
}
