package me.ezra_home.retail_software_solution.locations.business.catalog_sync

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class SyncStatus(override val code: String) : HasCode {
    IN_PROGRESS("I"),
    COMPLETED("C"),
    FAILED("F"),
    CANCELLATION_REQUESTED("XR"),
    CANCELLED("X")
}
