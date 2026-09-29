package me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class AdjustmentDirection(override val code: String) : HasCode {
    DISCOUNT("DISC"),
    SURCHARGE("SRCH"),
    BOTH("BOTH")
}
