package me.ezra_home.retail_software_solution.util.enums

enum class UserType(override val code: String) : HasCode {
  END_USER("E"),
  SERVICE_ACCOUNT("S")
}
