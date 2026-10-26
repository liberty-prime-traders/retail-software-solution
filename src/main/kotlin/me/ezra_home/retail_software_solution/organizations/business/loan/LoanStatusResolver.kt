package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanStatus
import java.math.BigDecimal

// Mirrors views/loan_status.yml. The view does not see the transaction's pending writes: read it once before writing
// and move it with these, never re-read it afterwards.
object LoanStatusResolver {

    fun forNewLoan(loanAmount: BigDecimal): LoanStatusFigures = resolve(BigDecimal.ZERO, loanAmount)

    fun afterRepayment(loanStatusFiguresBeforeRepayment: LoanStatusFigures, repaymentAmount: BigDecimal): LoanStatusFigures =
        resolve(
            loanStatusFiguresBeforeRepayment.amountRepaid + repaymentAmount,
            loanStatusFiguresBeforeRepayment.outstanding - repaymentAmount
        )

    fun afterRepaymentVoid(loanStatusFiguresBeforeVoid: LoanStatusFigures, repaymentAmount: BigDecimal): LoanStatusFigures =
        resolve(
            loanStatusFiguresBeforeVoid.amountRepaid - repaymentAmount,
            loanStatusFiguresBeforeVoid.outstanding + repaymentAmount
        )

    fun afterLoanVoid(loanStatusFiguresBeforeVoid: LoanStatusFigures): LoanStatusFigures =
        LoanStatusFigures(loanStatusFiguresBeforeVoid.amountRepaid, BigDecimal.ZERO, LoanStatus.VOIDED)

    private fun resolve(amountRepaid: BigDecimal, outstanding: BigDecimal): LoanStatusFigures = LoanStatusFigures(
        amountRepaid, outstanding, if (outstanding.signum() == 0) LoanStatus.SETTLED else LoanStatus.OPEN
    )
}
