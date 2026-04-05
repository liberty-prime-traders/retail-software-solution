package me.ezra_home.retail_software_solution.locations.business.catalog_sync.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class SyncInitiateType(override val code: String) : HasCode {
    USER("USR"),
    SCHEDULE("SCH")
}
