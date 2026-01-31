package me.ezra_home.retail_software_solution.util.enums

enum class SyncStatus(override val code: String) : HasCode {
  IN_PROGRESS("I"),
  COMPLETED("C"),
  FAILED("F"),
  CANCELLED("X")
}
