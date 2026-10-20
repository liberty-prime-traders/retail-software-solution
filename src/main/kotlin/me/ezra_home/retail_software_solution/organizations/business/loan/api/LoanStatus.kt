package me.ezra_home.retail_software_solution.organizations.business.loan.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class LoanStatus(override val code: String) : HasCode {
    OPEN("OPN"),
    SETTLED("STL"),
    VOIDED("VOI")
}
