package me.ezra_home.retail_software_solution.cucumber.support.assertions

import java.util.UUID

enum class SchemaScope {
  PLATFORM,
  ORGANIZATION,
}

interface PersistedResponseBinding {
  val alias: String
  val scope: SchemaScope

  fun responseDtoFor(id: UUID): Any
}
