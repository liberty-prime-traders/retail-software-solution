package me.ezra_home.retail_software_solution.util.business

import liquibase.command.CommandScope
import liquibase.command.core.UpdateCommandStep
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import javax.sql.DataSource

object SchemaCreator {

    fun createSchema(schemaName: String, dataSource: DataSource, changeLog: String) {
        try {
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("CREATE SCHEMA IF NOT EXISTS $schemaName")
                }
            }
        } catch (e: Exception) {
            dropSchema(schemaName, dataSource)
            throw RtsGenericException("Failed to create schema $schemaName: ${e.message}")
        }

        runMigration(schemaName, dataSource, changeLog)
    }

    private fun dropSchema(schemaName: String, dataSource: DataSource) {
        try {
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("DROP SCHEMA IF EXISTS $schemaName CASCADE")
                }
            }
        } catch (e: Exception) {
            println("Error dropping schema $schemaName: ${e.message}")
            throw RtsGenericException("Failed to drop schema $schemaName: ${e.message}")
        }

    }

    private fun runMigration(schemaName: String, dataSource: DataSource , changeLog: String) {
        try {
            val database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(dataSource.connection))
                .apply { defaultSchemaName = schemaName }
            val commandScope = CommandScope(UpdateCommandStep.COMMAND_NAME[0])
                .addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, changeLog)
                .addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database)
            commandScope.execute()

        } catch (e: Exception) {
            dropSchema(schemaName, dataSource)
            throw RtsGenericException("Failed to run migration for schema $schemaName: ${e.message}")
        }
    }
}
