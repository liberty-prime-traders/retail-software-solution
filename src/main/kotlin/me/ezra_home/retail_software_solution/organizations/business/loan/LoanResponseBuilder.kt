package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanAccountResponse
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanLenderResponse
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanRepaymentResponse
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanResponse
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanVoidResponse
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class LoanResponseBuilder(
    private val loanRepaymentRepository: LoanRepaymentRepository,
    private val loanStatusRepository: LoanStatusRepository,
    private val loanVoidRepository: LoanVoidRepository,
    private val loanRepaymentVoidRepository: LoanRepaymentVoidRepository,
    private val loanMapper: LoanMapper,
    private val accountDataFetcher: AccountDataFetcher,
    private val userQualifier: UserQualifier
) {

    fun buildResponse(loanDto: LoanDto, lenderContactDto: ContactDto): LoanResponse =
        buildResponses(listOf(loanDto), mapOf(lenderContactDto.id to lenderContactDto)).single()

    fun buildResponses(loanDtos: List<LoanDto>, contactsById: Map<UUID, ContactDto>): List<LoanResponse> {
        if (loanDtos.isEmpty()) return emptyList()
        val loanIds = loanDtos.map { it.id }
        val repaymentDtosByLoanId = loanRepaymentRepository.findByLoanIdIn(loanIds)
            .map { loanMapper.toRepaymentDto(it) }
            .sortedWith(compareBy({ it.createdOn }, { it.id }))
            .groupBy { it.loanId }
        val loanStatusEntitiesByLoanId = loanStatusRepository.findAllById(loanIds).associateBy { it.loanId }
        val loanVoidDtosByLoanId = loanVoidRepository.findByLoanIdIn(loanIds)
            .map { loanMapper.toVoidDto(it) }
            .associateBy { it.loanId }
        val repaymentVoidDtosByRepaymentId = loanRepaymentVoidRepository
            .findByLoanRepaymentIdIn(repaymentDtosByLoanId.values.flatten().map { it.id })
            .map { loanMapper.toRepaymentVoidDto(it) }
            .associateBy { it.loanRepaymentId }
        val accountCodes = loanDtos.map { it.assetAccountCode }.toSet() +
            repaymentDtosByLoanId.values.flatten().map { it.assetAccountCode }
        val accountLabelsByCode = accountDataFetcher.getLabelsByCodes(accountCodes)

        return loanDtos.map { loanDto ->
            val repaymentDtos = repaymentDtosByLoanId[loanDto.id].orEmpty()
            val loanVoidDto = loanVoidDtosByLoanId[loanDto.id]
            val loanStatusEntity = loanStatusEntitiesByLoanId.getValue(loanDto.id)
            val lenderContactDto = contactsById.getValue(loanDto.lenderContactId)
            LoanResponse(
                id = loanDto.id,
                referenceNumber = loanDto.referenceNumber,
                lender = LoanLenderResponse(lenderContactDto.referenceNumber, lenderContactDto.identity.displayName),
                assetAccount = toAccountResponse(loanDto.assetAccountCode, accountLabelsByCode),
                amount = loanDto.amount,
                loanDate = loanDto.loanDate,
                note = loanDto.note,
                amountRepaid = loanStatusEntity.amountRepaid,
                outstanding = loanStatusEntity.outstanding,
                status = loanStatusEntity.status,
                createdOn = loanDto.createdOn,
                createdBy = userQualifier.getCreatorFullName(loanDto.createdById),
                void = loanVoidDto?.let { toVoidResponse(it.voidDate, it.reason, it.createdById) },
                repayments = repaymentDtos.map { repaymentDto ->
                    LoanRepaymentResponse(
                        id = repaymentDto.id,
                        referenceNumber = repaymentDto.referenceNumber,
                        assetAccount = toAccountResponse(repaymentDto.assetAccountCode, accountLabelsByCode),
                        amount = repaymentDto.amount,
                        repaymentDate = repaymentDto.repaymentDate,
                        note = repaymentDto.note,
                        createdOn = repaymentDto.createdOn,
                        createdBy = userQualifier.getCreatorFullName(repaymentDto.createdById),
                        void = repaymentVoidDtosByRepaymentId[repaymentDto.id]
                            ?.let { toVoidResponse(it.voidDate, it.reason, it.createdById) }
                    )
                }
            )
        }
    }

    private fun toAccountResponse(accountCode: String, accountLabelsByCode: Map<String, String>) =
        LoanAccountResponse(accountCode, accountLabelsByCode.getValue(accountCode))

    private fun toVoidResponse(voidDate: LocalDate, reason: String?, voidedById: UUID) =
        LoanVoidResponse(voidDate, reason, userQualifier.getCreatorFullName(voidedById))
}
