package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity

data class LocationMigrationResults(
  val successful: List<DbMigrationEntity>,
  val failed: List<String>
) {
  companion object {
    fun empty() = LocationMigrationResults(emptyList(), emptyList())
  }
}
