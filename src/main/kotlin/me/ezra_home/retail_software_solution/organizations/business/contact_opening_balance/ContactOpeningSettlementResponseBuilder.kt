package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance

import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactOpeningSettlementResponse
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactOpeningSettlementSummaryResponse
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactOpeningSettlementVoidResponse
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.util.business.MoneyDisplay
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.springframework.stereotype.Component

@Component
class ContactOpeningSettlementResponseBuilder(
    private val contactOpeningSettlementRepository: ContactOpeningSettlementRepository,
    private val contactOpeningSettlementVoidRepository: ContactOpeningSettlementVoidRepository,
    private val contactOpeningSettlementFinder: ContactOpeningSettlementFinder,
    private val accountService: AccountService,
    private val userQualifier: UserQualifier
) {

    fun buildSummary(contactReferenceNumber: String): ContactOpeningSettlementSummaryResponse {
        val settlementEntities = contactOpeningSettlementRepository
            .findByContactReferenceNumberOrderByCreatedOnDescIdDesc(contactReferenceNumber)
        val voidEntitiesBySettlementId = contactOpeningSettlementVoidRepository
            .findBySettlementIdIn(settlementEntities.map { it.requiredId() })
            .associateBy { it.settlementId }
        val accountNamesByCode = accountService.getAccountNamesByCode()
        return ContactOpeningSettlementSummaryResponse(
            contactReferenceNumber = contactReferenceNumber,
            receivableOutstanding = MoneyDisplay.of(
                contactOpeningSettlementFinder
                    .outstandingAmount(contactReferenceNumber, ContactSubledgerDirection.RECEIVABLE)
            ),
            payableOutstanding = MoneyDisplay.of(
                contactOpeningSettlementFinder
                    .outstandingAmount(contactReferenceNumber, ContactSubledgerDirection.PAYABLE)
            ),
            settlements = settlementEntities.map { settlementEntity ->
                ContactOpeningSettlementResponse(
                    id = settlementEntity.requiredId(),
                    referenceNumber = settlementEntity.requiredReference(),
                    subledgerDirection = settlementEntity.subledgerDirection,
                    paymentAccountCode = settlementEntity.paymentAccountCode,
                    paymentAccountLabel = accountNamesByCode.getValue(settlementEntity.paymentAccountCode),
                    amount = MoneyDisplay.of(settlementEntity.amount),
                    settlementDate = settlementEntity.settlementDate,
                    note = settlementEntity.note,
                    recordedAt = settlementEntity.requiredCreatedOn(),
                    recordedBy = userQualifier.getUserFullName(settlementEntity.createdById) ?: "",
                    void = voidEntitiesBySettlementId[settlementEntity.requiredId()]?.let { voidEntity ->
                        ContactOpeningSettlementVoidResponse(
                            voidDate = voidEntity.voidDate,
                            reason = voidEntity.reason,
                            voidedBy = userQualifier.getUserFullName(voidEntity.createdById) ?: ""
                        )
                    }
                )
            }
        )
    }
}
