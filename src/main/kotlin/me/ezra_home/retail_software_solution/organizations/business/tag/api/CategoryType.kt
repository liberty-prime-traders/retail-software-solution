package me.ezra_home.retail_software_solution.organizations.business.tag.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class CategoryType(override val code: String) : HasCode {
    PRODUCT("PRD"),
    EXPENSE("EXP"),
    PURCHASE("PCH")
}
