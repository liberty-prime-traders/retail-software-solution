package me.ezra_home.retail_software_solution.util.enums

enum class SyncMode(override val code: String) : HasCode {
  FULL("F"),
  INCREMENTAL("I")
}
