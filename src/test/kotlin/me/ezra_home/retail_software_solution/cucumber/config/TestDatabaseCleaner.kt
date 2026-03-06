package me.ezra_home.retail_software_solution.cucumber.config

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

  fun clean() {
    val tables = jdbcTemplate.query(
      """
        SELECT table_schema, table_name
        FROM information_schema.tables
        WHERE table_type = 'BASE TABLE'
          AND table_schema NOT IN ('pg_catalog', 'information_schema')
          AND table_schema <> 'platform'
          AND table_name NOT IN ('databasechangelog', 'databasechangeloglock')
      """.trimIndent()
    ) { rs, _ ->
      "${quoteIdentifier(rs.getString("table_schema"))}.${quoteIdentifier(rs.getString("table_name"))}"
    }

    if (tables.isEmpty()) return

    val truncateSql = "TRUNCATE TABLE ${tables.joinToString(", ")} RESTART IDENTITY CASCADE"
    jdbcTemplate.execute(truncateSql)
  }

  private fun quoteIdentifier(identifier: String): String = "\"${identifier.replace("\"", "\"\"")}\""
}
