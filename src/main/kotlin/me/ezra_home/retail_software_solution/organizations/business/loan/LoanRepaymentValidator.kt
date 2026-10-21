package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class LoanRepaymentValidator(
    private val loanVoidRepository: LoanVoidRepository,
    private val loanStatusRepository: LoanStatusRepository,
    private val loanRepaymentVoidRepository: LoanRepaymentVoidRepository
) {

    fun requireRepayable(loanDto: LoanDto, repaymentDate: LocalDate, repaymentAmount: BigDecimal) {
        if (loanVoidRepository.existsByLoanId(loanDto.id)) {
            throw RtsGenericException("Loan ${loanDto.referenceNumber} is voided")
        }
        if (repaymentDate.isBefore(loanDto.loanDate)) {
            throw RtsGenericException("Repayment date cannot be before the loan date ${loanDto.loanDate}")
        }
        val outstanding = loanStatusRepository.findById(loanDto.id).orElseThrow { RtsGenericException("Loan status not found") }.outstanding
        if (repaymentAmount > outstanding) {
            throw RtsGenericException(
                "Repayment of $repaymentAmount exceeds the outstanding $outstanding on loan ${loanDto.referenceNumber}"
            )
        }
    }

    fun requireVoidable(loanRepaymentDto: LoanRepaymentDto) {
        if (loanRepaymentVoidRepository.existsByLoanRepaymentId(loanRepaymentDto.id)) {
            throw RtsGenericException("Loan repayment ${loanRepaymentDto.referenceNumber} is already voided")
        }
    }
}
