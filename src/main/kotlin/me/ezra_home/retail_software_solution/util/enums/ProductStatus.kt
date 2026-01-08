package me.ezra_home.retail_software_solution.util.enums

enum class ProductStatus(override val code: String) : HasCode {
    ACTIVE("A"),
    DISCONTINUED("X"),
    AWAITING_FINAL_SALE("AFS")
}
