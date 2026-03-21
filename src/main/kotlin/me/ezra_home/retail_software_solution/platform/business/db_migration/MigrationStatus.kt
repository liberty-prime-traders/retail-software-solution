package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class MigrationStatus(override val code: String) : HasCode {
    INITIATED("INITIATED"),
    SUCCESS("SUCCESS"),
    PARTIAL("PARTIAL"),
    IGNORED("IGNORED"),
    FAILURE("FAILURE")
}
