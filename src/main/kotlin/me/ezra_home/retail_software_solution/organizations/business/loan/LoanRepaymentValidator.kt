package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class LoanRepaymentValidator(
    private val loanVoidRepository: LoanVoidRepository,
    private val loanRepaymentVoidRepository: LoanRepaymentVoidRepository
) {

    fun requireRepayable(
        loanDto: LoanDto,
        loanStatusFiguresBeforeRepayment: LoanStatusFigures,
        repaymentDate: LocalDate,
        repaymentAmount: BigDecimal
    ) {
        if (loanVoidRepository.existsByLoanId(loanDto.id)) {
            throw RtsGenericException("Loan ${loanDto.referenceNumber} is voided")
        }
        if (repaymentDate.isBefore(loanDto.loanDate)) {
            throw RtsGenericException("Repayment date cannot be before the loan date ${loanDto.loanDate}")
        }
        if (repaymentAmount > loanStatusFiguresBeforeRepayment.outstanding) {
            throw RtsGenericException(
                "Repayment of $repaymentAmount exceeds the outstanding ${loanStatusFiguresBeforeRepayment.outstanding} on loan ${loanDto.referenceNumber}"
            )
        }
    }

    fun requireVoidable(loanRepaymentDto: LoanRepaymentDto) {
        if (loanRepaymentVoidRepository.existsByLoanRepaymentId(loanRepaymentDto.id)) {
            throw RtsGenericException("Loan repayment ${loanRepaymentDto.referenceNumber} is already voided")
        }
    }
}
