package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationDto

data class LocationMigrationResults(
  val successful: List<DbMigrationDto>,
  val failed: List<DbMigrationDto>
) {
  companion object {
    fun empty() = LocationMigrationResults(emptyList(), emptyList())
  }

  fun getFailedMessages(): String {
    return failed.joinToString("; ") { "(ID: ${it.id}): ${it.message ?: "Unknown error"}" }
  }

  fun getAllResults(): List<DbMigrationDto> = successful + failed
}
