package me.ezra_home.retail_software_solution.organizations.business.loan.api

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class LoanCreateRequest(
    val lenderContactReference: String,
    val assetAccountCode: String,
    val amount: BigDecimal,
    val loanDate: LocalDate,
    val note: String? = null
)

data class LoanRepaymentCreateRequest(
    val loanId: UUID,
    val assetAccountCode: String,
    val amount: BigDecimal,
    val repaymentDate: LocalDate,
    val note: String? = null
)

data class LoanVoidRequest(
    val loanId: UUID,
    val reason: String? = null
)

data class LoanRepaymentVoidRequest(
    val repaymentId: UUID,
    val reason: String? = null
)
