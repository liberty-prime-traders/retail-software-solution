package me.ezra_home.retail_software_solution.locations.business.catalog_sync.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class SyncMode(override val code: String) : HasCode {
    FULL("F"),
    INCREMENTAL("I")
}
