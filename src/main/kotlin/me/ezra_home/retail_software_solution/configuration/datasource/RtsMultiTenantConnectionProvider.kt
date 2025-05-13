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
                statement.execute("SET SCHEMA '$schemaName'") // PostgreSQL syntax
            }
        } catch (e: Exception) {
            connection.close()
            throw RuntimeException("Failed to set schema to '$schemaName'", e)
        }
        return connection
    }

    override fun releaseConnection(schemaName: String, connection: Connection) {
        connection.close()
    }

    override fun releaseAnyConnection(connection: Connection) {
        connection.close()
    }

    override fun supportsAggressiveRelease(): Boolean = true
    override fun isUnwrappableAs(unwrapType: Class<*>): Boolean = false
    override fun <T: Any?> unwrap(type: Class<T>): T? = null
}
