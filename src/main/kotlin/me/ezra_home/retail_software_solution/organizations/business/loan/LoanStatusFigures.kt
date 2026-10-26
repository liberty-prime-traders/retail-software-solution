package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanStatus
import java.math.BigDecimal

data class LoanStatusFigures(
    val amountRepaid: BigDecimal,
    val outstanding: BigDecimal,
    val status: LoanStatus
)
