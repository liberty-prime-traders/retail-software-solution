package me.ezra_home.retail_software_solution.util.enums

enum class CategoryType(override val code: String) : HasCode {
    PRODUCT ("PRD"),
    EXPENSE ("EXP"),
    PURCHASE ("PCH")
}
