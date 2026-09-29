package me.ezra_home.retail_software_solution.util.business

import liquibase.command.CommandScope
import liquibase.command.core.RollbackCommandStep
import liquibase.command.core.TagCommandStep
import liquibase.command.core.UpdateCommandStep
import liquibase.command.core.helpers.DatabaseChangelogCommandStep
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

        try {
            runMigration(
                schemaName = schemaName,
                dataSource = dataSource,
                changeLog = changeLog,
            )
        } catch (e: Exception) {
            dropSchema(schemaName, dataSource)
            throw RtsGenericException("Failed to run migration for schema $schemaName: ${e.message}")
        }

    }

    fun dropSchema(schemaName: String, dataSource: DataSource) {
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

    fun runMigration(
        schemaName: String,
        dataSource: DataSource,
        changeLog: String,
        targetVersion: String? = null,
        previousVersion: String? = null
    ) {
        try {
            dataSource.connection.use { conn ->
                val database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(JdbcConnection(conn))
                    .apply {
                        defaultSchemaName = schemaName
                        outputDefaultSchema = true
                    }
                val commandScope = CommandScope(UpdateCommandStep.COMMAND_NAME[0])
                    .addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, changeLog)
                    .addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database)
                    .addArgumentValue(UpdateCommandStep.LABEL_FILTER_ARG, targetVersion)
                commandScope.execute()
            }
        } catch (_: Exception) {
            rollbackMigration(schemaName, dataSource, changeLog, previousVersion)
            throw RtsGenericException("Failed to run migration for schema $schemaName")
        }
    }

    // TODO Properly tag each org and location after migration so this can be used to restore
    fun tagDatabase(schemaName: String, dataSource: DataSource, version: String) {
        dataSource.connection.use { conn ->
            val database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(conn))
                .apply {
                    defaultSchemaName = schemaName
                    outputDefaultSchema = true
                }
            CommandScope("tag")
                .addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database)
                .addArgumentValue(TagCommandStep.TAG_ARG, version)
                .execute()
        }
    }

    fun rollbackMigration(schemaName: String, dataSource: DataSource, changeLog: String, previousVersion: String?) {
        if (previousVersion.isNullOrBlank()) return
        try {
            dataSource.connection.use { conn ->
                val database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(JdbcConnection(conn))
                    .apply {
                        defaultSchemaName = schemaName
                        outputDefaultSchema = true
                    }
                CommandScope(RollbackCommandStep.COMMAND_NAME[0])
                    .addArgumentValue(DatabaseChangelogCommandStep.CHANGELOG_FILE_ARG, changeLog)
                    .addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database)
                    .addArgumentValue(RollbackCommandStep.TAG_ARG, previousVersion)
                    .execute()
            }
        } catch (_: Exception) {
            throw RtsGenericException("Failed to run migration for schema $schemaName. Additionally, the schema failed to rollback")
        }
    }
}
