package me.ezra_home.retail_software_solution.util.enums

enum class MigrationResult(override val code: String) : HasCode {
    SUCCESS("SUCCESS"),
    PARTIAL("PARTIAL"),
    FAILURE("FAILURE")
}
