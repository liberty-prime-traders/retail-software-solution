package me.ezra_home.retail_software_solution.util.enums

enum class CalculationMethod(override val code: String) : HasCode {
    PERCENTAGE("PCT"),
    FLAT_PER_UNIT("FPU")
}
