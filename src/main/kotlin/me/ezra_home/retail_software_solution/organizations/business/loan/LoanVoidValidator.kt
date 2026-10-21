package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class LoanVoidValidator(
    private val loanVoidRepository: LoanVoidRepository,
    private val activeLoanRepaymentRepository: ActiveLoanRepaymentRepository
) {

    fun requireVoidable(loanDto: LoanDto) {
        if (loanVoidRepository.existsByLoanId(loanDto.id)) {
            throw RtsGenericException("Loan ${loanDto.referenceNumber} is already voided")
        }
        val activeRepaymentReferences = activeLoanRepaymentRepository.findByLoanIdOrderByCreatedOnAscIdAsc(loanDto.id).map { it.referenceNumber }
        if (activeRepaymentReferences.isNotEmpty()) {
            throw RtsGenericException(
                "Loan ${loanDto.referenceNumber} cannot be voided while it has repayments: ${activeRepaymentReferences.joinToString()}"
            )
        }
    }
}
