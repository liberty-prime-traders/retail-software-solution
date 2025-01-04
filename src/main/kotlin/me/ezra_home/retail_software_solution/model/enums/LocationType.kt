package me.ezra_home.retail_software_solution.model.enums

import me.ezra_home.retail_software_solution.model.util.HasCode

enum class LocationType(override val code: String) : HasCode {
    SHOP("SHP"),
    STORE("STR")
}
