package me.ezra_home.retail_software_solution.util.enums

enum class MovementReason(override val code: String) : HasCode {
    DAMAGED("DMG"),
    QUALITY_ISSUE("QI"),
    SURPLUS("SUR"),
    THEFT("THF"),
    COUNT_DISCREPANCY("CD"),
    EXPIRED("EXP")
}
