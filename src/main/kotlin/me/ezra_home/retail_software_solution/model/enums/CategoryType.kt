package me.ezra_home.retail_software_solution.model.enums

import me.ezra_home.retail_software_solution.model.util.HasCode

enum class CategoryType(override val code: String) : HasCode {
    PRODUCT ("PRD"),
    EXPENSE ("EXP"),
    PURCHASE ("PCH")
}