package me.ezra_home.retail_software_solution.util.enums

enum class MigrationStatus(override val code: String) : HasCode {
    INITIATED("INITIATED"),
    SUCCESS("SUCCESS"),
    PARTIAL("PARTIAL"),
    IGNORED("IGNORED"),
    FAILURE("FAILURE")
}
