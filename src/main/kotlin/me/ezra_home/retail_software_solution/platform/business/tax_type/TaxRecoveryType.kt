package me.ezra_home.retail_software_solution.platform.business.tax_type

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class TaxRecoveryType(override val code: String) : HasCode {
    RECOVERABLE("REC"),
    NON_RECOVERABLE("NREC")
}
