package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactStatus
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.ContactOpeningBalanceEntity
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.ContactOpeningBalancePostingRequests
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.ContactOpeningBalanceRepository
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.ContactOpeningSettlementFinder
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerBalanceDto
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.DisplayFormatters
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ContactOpeningBalanceService(
    private val contactOpeningBalanceRepository: ContactOpeningBalanceRepository,
    private val contactService: ContactService,
    private val ledgerPostingService: LedgerPostingService,
    private val contactOpeningSettlementFinder: ContactOpeningSettlementFinder,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock,
    private val fiscalPeriodService: FiscalPeriodService,
    private val userQualifier: UserQualifier
) {

    fun set(contactId: UUID, contactOpeningBalanceSetDto: ContactOpeningBalanceSetDto) {
        val postingDate = DateTimes.Local.Now.organization()
        fiscalPeriodService.requireOpenForDate(postingDate)
        val contactDto = contactService.getContactById(contactId)
        if (contactDto.status != ContactStatus.ACTIVE) {
            throw RtsGenericException("Only an active contact can take an opening balance")
        }
        orgEntityAdvisoryLock.acquire(LockNamespaces.CONTACT_OPENING_BALANCE, contactDto.referenceNumber)
        val existingBalance = SubledgerBalanceDto(
            payable = contactOpeningSettlementFinder.declaredAmount(
                contactDto.referenceNumber,
                ContactSubledgerDirection.PAYABLE
            ),
            receivable = contactOpeningSettlementFinder.declaredAmount(
                contactDto.referenceNumber,
                ContactSubledgerDirection.RECEIVABLE
            )
        )
        val targetBalance = contactOpeningBalanceSetDto.applyTo(existingBalance)
        requireNotBelowSettled(
            contactDto.referenceNumber,
            ContactSubledgerDirection.RECEIVABLE,
            targetBalance.receivable
        )
        requireNotBelowSettled(
            contactDto.referenceNumber,
            ContactSubledgerDirection.PAYABLE,
            targetBalance.payable
        )
        reviseSubledgerDirection(
            contactDto.referenceNumber, ContactSubledgerDirection.RECEIVABLE,
            existingBalance.receivable, targetBalance.receivable, postingDate
        )
        reviseSubledgerDirection(
            contactDto.referenceNumber, ContactSubledgerDirection.PAYABLE,
            existingBalance.payable, targetBalance.payable, postingDate
        )
    }

    private fun requireNotBelowSettled(
        contactReferenceNumber: String,
        subledgerDirection: ContactSubledgerDirection,
        targetAmount: BigDecimal
    ) {
        val settledAmount = contactOpeningSettlementFinder.activeSettledAmount(contactReferenceNumber, subledgerDirection)
        if (targetAmount < settledAmount) {
            throw RtsGenericException(
                "The ${subledgerDirection.name.lowercase()} opening balance cannot be set to " +
                    "${DisplayFormatters.formatCurrency(targetAmount)}, below the " +
                    "${DisplayFormatters.formatCurrency(settledAmount)} already settled against it; " +
                    "void the settlements first"
            )
        }
    }

    private fun reviseSubledgerDirection(
        contactReferenceNumber: String,
        subledgerDirection: ContactSubledgerDirection,
        existingAmount: BigDecimal,
        targetAmount: BigDecimal,
        postingDate: LocalDate
    ) {
        val signedDelta = targetAmount - existingAmount
        if (signedDelta.compareTo(BigDecimal.ZERO) == 0) return

        val savedContactOpeningBalanceEntity = contactOpeningBalanceRepository.save(
            ContactOpeningBalanceEntity(
                contactReferenceNumber = contactReferenceNumber,
                subledgerDirection = subledgerDirection,
                amount = targetAmount
            )
        )
        ledgerPostingService.post(
            ContactOpeningBalancePostingRequests.revised(
                ledgerSourceReferenceNumber = savedContactOpeningBalanceEntity.requiredReference(),
                contactReferenceNumber = contactReferenceNumber,
                subledgerDirection = subledgerDirection,
                signedDelta = signedDelta,
                postingDate = postingDate
            )
        )
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getHistory(contactReferenceNumber: String): List<ContactOpeningBalanceRevisionDto> =
        contactOpeningBalanceRepository.findByContactReferenceNumberOrderByCreatedOnAsc(contactReferenceNumber)
            .map { revisionRow ->
                ContactOpeningBalanceRevisionDto(
                    referenceNumber = revisionRow.requiredReference(),
                    contactReferenceNumber = revisionRow.contactReferenceNumber,
                    subledgerDirection = revisionRow.subledgerDirection,
                    amount = revisionRow.amount,
                    balanceEffect = claimedBalanceEffect(revisionRow),
                    changedAt = revisionRow.requiredCreatedOn(),
                    changedBy = userQualifier.getUserFullName(revisionRow.createdById) ?: ""
                )
            }

    private fun claimedBalanceEffect(revisionRow: ContactOpeningBalanceEntity): BalanceEffect = BalanceEffect.ofChange(
        revisionRow.amount,
        if (revisionRow.subledgerDirection == ContactSubledgerDirection.RECEIVABLE) BalanceEffect.ADDS else BalanceEffect.SUBTRACTS
    )
}
