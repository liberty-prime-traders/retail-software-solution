package me.ezra_home.retail_software_solution.configuration.datasource

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider
import java.sql.Connection
import javax.sql.DataSource

class RtsMultiTenantConnectionProvider(private val dataSource: DataSource): MultiTenantConnectionProvider<String> {

    override fun getAnyConnection(): Connection = dataSource.connection

    override fun getConnection(schemaName: String): Connection {
        val connection = getAnyConnection()
        try {
            connection.createStatement().use { statement ->
                statement.execute("SET SCHEMA '$schemaName'")
                statement.execute("SET search_path = '$schemaName', public")
            }
        } catch (e: Exception) {
            connection.close()
            throw RuntimeException("Failed to set schema to '$schemaName'", e)
        }
        return connection
    }

    override fun releaseConnection(schemaName: String, connection: Connection) {
        connection.use { conn ->
            conn.createStatement().use { statement ->
                statement.execute("SET SCHEMA '${DataSourceBeanNames.PLATFORM_SCHEMA_NAME}'")
                statement.execute("SET search_path = '${DataSourceBeanNames.PLATFORM_SCHEMA_NAME}', public")
            }
        }
    }

    override fun releaseAnyConnection(connection: Connection) {
        connection.close()
    }

    override fun supportsAggressiveRelease(): Boolean = false
    override fun isUnwrappableAs(unwrapType: Class<*>): Boolean = false
    override fun <T> unwrap(type: Class<T>): T? = null
}
