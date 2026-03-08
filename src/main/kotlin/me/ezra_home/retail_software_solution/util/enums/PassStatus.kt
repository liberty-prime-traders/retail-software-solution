package me.ezra_home.retail_software_solution.util.enums

enum class PassStatus(override val code: String) : HasCode {
  ACTIVE("A"),
  REVOKED("R"),
  EXHAUSTED("E"),
  EXPIRED("X")
}
