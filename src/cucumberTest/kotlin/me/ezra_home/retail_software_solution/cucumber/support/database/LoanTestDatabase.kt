package me.ezra_home.retail_software_solution.cucumber.support.database

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.support.TestConstants
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class LoanTestDatabase(
  @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_DATA_SOURCE)
  organizationDataSource: DataSource
) {

  private val jdbcTemplate = JdbcTemplate(organizationDataSource)

  fun countLedgerGroups(sourceReferenceNumber: String, sourceTypeCode: String): Int =
    requireNotNull(
      jdbcTemplate.queryForObject(
        "SELECT count(*) FROM \"${TestConstants.Seed.ORG_SCHEMA}\".ledger_entry_group WHERE source_reference_number = ? AND source_type = ?",
        Int::class.java,
        sourceReferenceNumber,
        sourceTypeCode
      )
    )

  fun countLedgerGroupsOfType(sourceTypeCode: String): Int =
    requireNotNull(
      jdbcTemplate.queryForObject(
        "SELECT count(*) FROM \"${TestConstants.Seed.ORG_SCHEMA}\".ledger_entry_group WHERE source_type = ?",
        Int::class.java,
        sourceTypeCode
      )
    )

  fun countLedgerGroupsWithLocation(sourceReferenceNumber: String): Int =
    requireNotNull(
      jdbcTemplate.queryForObject(
        "SELECT count(*) FROM \"${TestConstants.Seed.ORG_SCHEMA}\".ledger_entry_group WHERE source_reference_number = ? AND source_location_id IS NOT NULL",
        Int::class.java,
        sourceReferenceNumber
      )
    )
}
