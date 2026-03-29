package me.ezra_home.retail_software_solution.platform.business.tax_type

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class CalculationMethod(override val code: String) : HasCode {
    PERCENTAGE("PCT"),
    FLAT_PER_UNIT("FPU")
}
