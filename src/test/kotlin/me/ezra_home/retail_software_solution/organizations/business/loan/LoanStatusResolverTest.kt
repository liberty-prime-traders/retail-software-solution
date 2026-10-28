package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class LoanStatusResolverTest {

    private val openLoanStatusFigures = LoanStatusFigures(BigDecimal("40"), BigDecimal("60"), LoanStatus.OPEN)

    @Test
    fun `a new loan is open with its whole amount outstanding`() {
        val loanStatusFigures = LoanStatusResolver.forNewLoan(BigDecimal("100"))

        assertEquals(LoanStatusFigures(BigDecimal.ZERO, BigDecimal("100"), LoanStatus.OPEN), loanStatusFigures)
    }

    @Test
    fun `a partial repayment keeps the loan open and a full one settles it`() {
        val partiallyRepaid = LoanStatusResolver.afterRepayment(openLoanStatusFigures, BigDecimal("10"))
        val fullyRepaid = LoanStatusResolver.afterRepayment(openLoanStatusFigures, BigDecimal("60"))

        assertEquals(LoanStatusFigures(BigDecimal("50"), BigDecimal("50"), LoanStatus.OPEN), partiallyRepaid)
        assertEquals(LoanStatusFigures(BigDecimal("100"), BigDecimal("0"), LoanStatus.SETTLED), fullyRepaid)
    }

    @Test
    fun `voiding a repayment reopens a settled loan`() {
        val settledLoanStatusFigures = LoanStatusFigures(BigDecimal("100"), BigDecimal("0"), LoanStatus.SETTLED)

        val reopened = LoanStatusResolver.afterRepaymentVoid(settledLoanStatusFigures, BigDecimal("30"))

        assertEquals(LoanStatusFigures(BigDecimal("70"), BigDecimal("30"), LoanStatus.OPEN), reopened)
    }

    @Test
    fun `voiding a loan zeroes the outstanding balance and keeps the amount repaid`() {
        val voided = LoanStatusResolver.afterLoanVoid(openLoanStatusFigures)

        assertEquals(LoanStatusFigures(BigDecimal("40"), BigDecimal.ZERO, LoanStatus.VOIDED), voided)
    }
}
