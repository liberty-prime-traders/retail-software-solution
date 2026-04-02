package me.ezra_home.retail_software_solution.cucumber.support

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class TestDatabaseCleaner(
  @Qualifier(DataSourceBeanNames.PLATFORM_SCHEMA_DATA_SOURCE)
  dataSource: DataSource
) {
  private val jdbcTemplate = JdbcTemplate(dataSource)
  private var cachedTableKey: String? = null
  private var cachedTruncateSql: String? = null

  companion object {
    private val PROTECTED_SCHEMAS = setOf("pg_catalog", "information_schema", DataSourceBeanNames.PLATFORM_SCHEMA_NAME)
    private val PROTECTED_TABLES = setOf("databasechangelog", "databasechangeloglock")
  }

  fun clean() {
    val truncateSql = getOrBuildTruncateSql() ?: return
    jdbcTemplate.execute(truncateSql)
  }

  private fun getOrBuildTruncateSql(): String? {
    val schemaList = PROTECTED_SCHEMAS.joinToString(",") { "'$it'" }
    val tableList = PROTECTED_TABLES.joinToString(",") { "'$it'" }
    val scope = "WHERE table_type = 'BASE TABLE' AND table_schema NOT IN ($schemaList) AND table_name NOT IN ($tableList)"

    val tables = jdbcTemplate.query(
      "SELECT table_schema, table_name FROM information_schema.tables $scope ORDER BY table_schema, table_name"
    ) { rs, _ ->
      "${quoteIdentifier(rs.getString("table_schema"))}.${quoteIdentifier(rs.getString("table_name"))}"
    }

    val tableKey = tables.joinToString(",")
    if (tableKey == cachedTableKey) return cachedTruncateSql

    cachedTableKey = tableKey
    cachedTruncateSql = if (tables.isEmpty()) null
    else "TRUNCATE TABLE ${tables.joinToString(", ")} RESTART IDENTITY CASCADE"
    return cachedTruncateSql
  }

  private fun quoteIdentifier(identifier: String): String = "\"${identifier.replace("\"", "\"\"")}\""
}
