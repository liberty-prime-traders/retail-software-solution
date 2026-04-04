package me.ezra_home.retail_software_solution.cucumber.support.cleanup

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class TestDatabaseCleaner(
  @Qualifier(DataSourceBeanNames.PLATFORM_SCHEMA_DATA_SOURCE)
  platformDataSource: DataSource,

  @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_DATA_SOURCE)
  orgDataSource: DataSource,

  @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_DATA_SOURCE)
  locationDataSource: DataSource
) {

  private val templates = mapOf(
    DataSourceBeanNames.PLATFORM_SCHEMA_NAME to JdbcTemplate(platformDataSource),
    TestConstants.Seed.ORG_SCHEMA to JdbcTemplate(orgDataSource),
    TestConstants.Seed.LOCATION_SCHEMA to JdbcTemplate(locationDataSource)
  )

  private val sqlCache = mutableMapOf<String, String?>()

  companion object {
    private val PROTECTED_SCHEMAS = setOf("pg_catalog", "information_schema")
    private val PROTECTED_TABLES = setOf(
      "databasechangelog",
      "databasechangeloglock",

      TableNames.DB_VERSION,
      TableNames.SYS_USER,
      TableNames.RESERVED_SUBDOMAIN,
      TableNames.TABLE_REGISTRY,
      TableNames.ORGANIZATION,
      TableNames.ORGANIZATION_AUDIT,
      TableNames.AUTHORIZATION_PASS,
      TableNames.AUTHORIZATION_PASS_AUDIT,

      TableNames.LOCATION,
      TableNames.LOCATION_AUDIT
    )
  }

  fun clean() {
    templates.forEach { (schemaName, template) ->
      sqlCache.getOrPut(schemaName) { buildTruncateSql(template) }?.let { template.execute(it) }
    }
  }

  private fun buildTruncateSql(template: JdbcTemplate): String? {
    val schemaList = PROTECTED_SCHEMAS.joinToString(",") { "'$it'" }
    val tableList = PROTECTED_TABLES.joinToString(",") { "'$it'" }
    val scope = "WHERE table_type = 'BASE TABLE' AND table_schema NOT IN ($schemaList) AND table_name NOT IN ($tableList)"

    val tables = template.query(
      "SELECT table_schema, table_name FROM information_schema.tables $scope ORDER BY table_schema, table_name"
    ) { rs, _ ->
      "${quoteIdentifier(rs.getString("table_schema"))}.${quoteIdentifier(rs.getString("table_name"))}"
    }

    return if (tables.isEmpty()) null
    else "TRUNCATE TABLE ${tables.joinToString(", ")} RESTART IDENTITY CASCADE"
  }

  private fun quoteIdentifier(identifier: String): String {
    return "\"${identifier.replace("\"", "\"\"")}\""
  }
}
