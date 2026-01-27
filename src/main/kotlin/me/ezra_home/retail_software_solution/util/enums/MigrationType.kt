package me.ezra_home.retail_software_solution.util.enums

enum class MigrationType(override val code: String) : HasCode {
  ORG_ONLY("ORG_ONLY"),
  ORG_WITH_LOCATIONS("ORG_WITH_LOCATIONS"),
  LOCATIONS_ONLY("LOCATIONS")
}
