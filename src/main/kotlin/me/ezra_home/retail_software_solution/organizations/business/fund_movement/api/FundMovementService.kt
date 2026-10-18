package me.ezra_home.retail_software_solution.organizations.business.fund_movement.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.FundMovementAccountValidator
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.FundMovementEntity
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.FundMovementMapper
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.FundMovementRepository
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.FundMovementResponseBuilder
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.FundMovementVoidEntity
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.FundMovementVoidRepository
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.FundMovementWithVoid
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.FundMovementPostingRequests
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
@TransactionalOnOrganizationSchema
class FundMovementService(
    private val fundMovementRepository: FundMovementRepository,
    private val fundMovementVoidRepository: FundMovementVoidRepository,
    private val fundMovementMapper: FundMovementMapper,
    private val fundMovementAccountValidator: FundMovementAccountValidator,
    private val fundMovementResponseBuilder: FundMovementResponseBuilder,
    private val ledgerPostingService: LedgerPostingService,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock
) {

    fun createTransfer(fundMovementTransferRequest: FundMovementTransferRequest): FundMovementResponse {
        val fromAccountCode = StringUtils.getValueOrException(fundMovementTransferRequest.fromAccountCode, "From account is required")
        val toAccountCode = StringUtils.getValueOrException(fundMovementTransferRequest.toAccountCode, "To account is required")
        if (fromAccountCode == toAccountCode) {
            throw RtsGenericException("Cannot transfer funds from an account to itself")
        }
        return record(
            FundMovementType.TRANSFER_BETWEEN_ACCOUNTS,
            debitAccountCode = toAccountCode,
            creditAccountCode = fromAccountCode,
            userPickedAccountCodes = listOf(fromAccountCode, toAccountCode),
            amount = fundMovementTransferRequest.amount,
            movementDate = fundMovementTransferRequest.movementDate,
            note = fundMovementTransferRequest.note
        )
    }

    fun createCapital(fundMovementCapitalRequest: FundMovementCapitalRequest): FundMovementResponse {
        val accountCode = StringUtils.getValueOrException(fundMovementCapitalRequest.accountCode, "Account is required")
        return record(
            FundMovementType.CONTRIBUTE_CAPITAL_TO_BUSINESS,
            debitAccountCode = accountCode,
            creditAccountCode = SystemAccount.OWNERS_CAPITAL.code,
            userPickedAccountCodes = listOf(accountCode),
            amount = fundMovementCapitalRequest.amount,
            movementDate = fundMovementCapitalRequest.movementDate,
            note = fundMovementCapitalRequest.note
        )
    }

    fun createDraw(fundMovementDrawRequest: FundMovementDrawRequest): FundMovementResponse {
        val accountCode = StringUtils.getValueOrException(fundMovementDrawRequest.accountCode, "Account is required")
        return record(
            FundMovementType.WITHDRAW_FOR_PERSONAL_USE,
            debitAccountCode = SystemAccount.OWNERS_DRAWS.code,
            creditAccountCode = accountCode,
            userPickedAccountCodes = listOf(accountCode),
            amount = fundMovementDrawRequest.amount,
            movementDate = fundMovementDrawRequest.movementDate,
            note = fundMovementDrawRequest.note
        )
    }

    fun voidFundMovement(fundMovementVoidRequest: FundMovementVoidRequest): FundMovementResponse {
        val reason = limitedText(fundMovementVoidRequest.reason, "Reason")
        orgEntityAdvisoryLock.acquire(LockNamespaces.FUND_MOVEMENT, fundMovementVoidRequest.id.toString())
        val fundMovementEntity = fundMovementRepository.findById(fundMovementVoidRequest.id)
            .orElseThrow { RtsGenericException("Fund movement not found") }
        if (fundMovementVoidRepository.existsByFundMovementId(fundMovementVoidRequest.id)) {
            throw RtsGenericException("Fund movement ${fundMovementEntity.requiredReference()} is already voided")
        }
        val fundMovementDto = fundMovementMapper.toDto(fundMovementEntity)
        val fundMovementVoidDto = fundMovementMapper.toVoidDto(
            fundMovementVoidRepository.save(
                FundMovementVoidEntity(fundMovementDto.id, DateTimes.Local.Now.organization(), reason)
            )
        )
        ledgerPostingService.post(FundMovementPostingRequests.voided(
                fundMovementDto.referenceNumber, fundMovementDto.debitAccountCode, fundMovementDto.creditAccountCode,
                fundMovementDto.amount, fundMovementVoidDto.voidDate
            ))
        return fundMovementResponseBuilder.buildResponses(listOf(FundMovementWithVoid(fundMovementDto, fundMovementVoidDto))).single()
    }

    private fun record(
        type: FundMovementType,
        debitAccountCode: String,
        creditAccountCode: String,
        userPickedAccountCodes: List<String>,
        amount: BigDecimal,
        movementDate: LocalDate,
        note: String?
    ): FundMovementResponse {
        val roundedAmount = Decimals.roundToScale4(amount)
        if (roundedAmount.signum() <= 0) {
            throw RtsGenericException("Amount must be greater than zero")
        }
        if (movementDate.isAfter(DateTimes.Local.Now.organization())) {
            throw RtsGenericException("Movement date cannot be in the future")
        }
        val trimmedNote = limitedText(note, "Note")
        fundMovementAccountValidator.requireSelectable(userPickedAccountCodes)
        val fundMovementDto = fundMovementMapper.toDto(
            fundMovementRepository.save(
                FundMovementEntity(type, debitAccountCode, creditAccountCode, roundedAmount, movementDate, trimmedNote)
            )
        )
        ledgerPostingService.post(FundMovementPostingRequests.recorded(
                fundMovementDto.referenceNumber, fundMovementDto.debitAccountCode, fundMovementDto.creditAccountCode,
                fundMovementDto.amount, fundMovementDto.movementDate
            ))
        return fundMovementResponseBuilder.buildResponses(listOf(FundMovementWithVoid(fundMovementDto, null))).single()
    }

    private fun limitedText(text: String?, label: String): String? {
        val normalizedText = StringUtils.getValueOrNull(text)
        if (normalizedText != null && normalizedText.length > MAXIMUM_TEXT_LENGTH) {
            throw RtsGenericException("$label must be at most $MAXIMUM_TEXT_LENGTH characters")
        }
        return normalizedText
    }

    private companion object {
        const val MAXIMUM_TEXT_LENGTH = 500
    }
}
