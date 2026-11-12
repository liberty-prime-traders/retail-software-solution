package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance

import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class ContactOpeningSettlementFinder(
    private val contactOpeningBalanceRepository: ContactOpeningBalanceRepository,
    private val activeContactOpeningSettlementRepository: ActiveContactOpeningSettlementRepository
) {

    fun declaredAmount(contactReferenceNumber: String, subledgerDirection: ContactSubledgerDirection): BigDecimal =
        contactOpeningBalanceRepository.findLatestRevision(
            contactReferenceNumber,
            subledgerDirection
        )?.amount ?: BigDecimal.ZERO

    fun activeSettledAmount(contactReferenceNumber: String, subledgerDirection: ContactSubledgerDirection): BigDecimal =
        activeContactOpeningSettlementRepository.sumAmount(contactReferenceNumber, subledgerDirection)

    fun outstandingAmount(contactReferenceNumber: String, subledgerDirection: ContactSubledgerDirection): BigDecimal =
        declaredAmount(contactReferenceNumber, subledgerDirection) -
            activeSettledAmount(contactReferenceNumber, subledgerDirection)
}
