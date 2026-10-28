package me.ezra_home.retail_software_solution.organizations.business.loan.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanAccountValidator
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanEntity
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanFinder
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanLenderValidator
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanMapper
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanPostingRequests
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanRepository
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanResponseBuilder
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanStatusResolver
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanVoidEntity
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanVoidRepository
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanVoidValidator
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema
class LoanService(
    private val loanRepository: LoanRepository,
    private val loanVoidRepository: LoanVoidRepository,
    private val loanMapper: LoanMapper,
    private val loanFinder: LoanFinder,
    private val loanLenderValidator: LoanLenderValidator,
    private val loanAccountValidator: LoanAccountValidator,
    private val loanVoidValidator: LoanVoidValidator,
    private val loanResponseBuilder: LoanResponseBuilder,
    private val ledgerPostingService: LedgerPostingService
) {

    fun createLoan(loanCreateRequest: LoanCreateRequest): LoanResponse {
        val loanAmount = loanCreateRequest.amount.toBigDecimal()
        val assetAccountCode = StringUtils.getValueOrException(loanCreateRequest.assetAccountCode, "Asset account is required")
        DateTimes.Local.requireNotInFuture(loanCreateRequest.loanDate, "Loan date")
        val trimmedNote = StringUtils.getValueOrNull(loanCreateRequest.note)
        val lenderContactDto = loanLenderValidator.requireActiveLender(loanCreateRequest.lenderContactReference)
        loanAccountValidator.requireSelectable(assetAccountCode)
        val loanDto = loanMapper.toDto(
            loanRepository.save(
                LoanEntity(lenderContactDto.id, assetAccountCode, loanAmount, loanCreateRequest.loanDate, trimmedNote)
            )
        )
        ledgerPostingService.post(
            LoanPostingRequests.loanRecorded(
                loanDto.referenceNumber, loanDto.assetAccountCode, lenderContactDto.referenceNumber,
                loanDto.amount, loanDto.loanDate
            )
        )
        return loanResponseBuilder.buildResponse(loanDto, lenderContactDto, LoanStatusResolver.forNewLoan(loanDto.amount))
    }

    fun voidLoan(loanVoidRequest: LoanVoidRequest): LoanResponse {
        val reason = StringUtils.getValueOrNull(loanVoidRequest.reason)
        loanFinder.lockLoan(loanVoidRequest.loanId)
        val loanDto = loanFinder.findLoan(loanVoidRequest.loanId)
        loanVoidValidator.requireVoidable(loanDto)
        val lenderContactDto = loanFinder.findLender(loanDto)
        val loanStatusFiguresBeforeVoid = loanFinder.findLoanStatusFigures(loanDto.id)
        val loanVoidDto = loanMapper.toVoidDto(
            loanVoidRepository.save(LoanVoidEntity(loanDto.id, DateTimes.Local.Now.organization(), reason))
        )
        ledgerPostingService.post(
            LoanPostingRequests.loanVoided(
                loanDto.referenceNumber, loanDto.assetAccountCode, lenderContactDto.referenceNumber,
                loanDto.amount, loanVoidDto.voidDate
            )
        )
        return loanResponseBuilder.buildResponse(loanDto, lenderContactDto, LoanStatusResolver.afterLoanVoid(loanStatusFiguresBeforeVoid))
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getLoan(loanReference: String): LoanResponse {
        val loanEntity = loanRepository.findByReferenceNumber(loanReference)
            ?: throw RtsGenericException("Loan not found: $loanReference")
        val loanDto = loanMapper.toDto(loanEntity)
        val lenderContactDto = loanFinder.findLender(loanDto)
        return loanResponseBuilder.buildResponses(listOf(loanDto), mapOf(lenderContactDto.id to lenderContactDto)).single()
    }
}
