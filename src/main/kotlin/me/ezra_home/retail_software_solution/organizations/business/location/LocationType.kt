package me.ezra_home.retail_software_solution.organizations.business.location

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class LocationType(override val code: String) : HasCode {
    SHOP("SHP"),
    STORE("STR")
}
