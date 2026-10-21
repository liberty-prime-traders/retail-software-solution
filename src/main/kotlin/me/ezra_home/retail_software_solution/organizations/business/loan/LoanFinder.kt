package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class LoanFinder(
    private val loanRepository: LoanRepository,
    private val loanMapper: LoanMapper,
    private val contactService: ContactService,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock
) {

    fun lockLoan(loanId: UUID) {
        orgEntityAdvisoryLock.acquire(LockNamespaces.LOAN, loanId.toString())
    }

    fun findLoan(loanId: UUID): LoanDto = loanMapper.toDto(
        loanRepository.findById(loanId).orElseThrow { RtsGenericException("Loan not found") }
    )

    fun findLender(loanDto: LoanDto): ContactDto = contactService.getContactById(loanDto.lenderContactId)
}
