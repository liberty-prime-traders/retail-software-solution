package me.ezra_home.retail_software_solution.platform.business.tax_type

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class TaxApplicationLevel(override val code: String) : HasCode {
    PRODUCT("PRD"),
    ORGANIZATION("ORG")
}
