package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.ContactOpeningBalanceRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerBalanceDto
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
@TransactionalOnOrganizationSchema(readOnly = true)
class ContactOpeningBalanceReader(
    private val contactOpeningBalanceRepository: ContactOpeningBalanceRepository
) {

    fun getOpeningBalances(contactReferenceNumbers: Set<String>): Map<String, SubledgerBalanceDto> {
        if (contactReferenceNumbers.isEmpty()) return emptyMap()
        return contactOpeningBalanceRepository.findLatestForContacts(contactReferenceNumbers)
            .groupBy { it.contactReferenceNumber }
            .mapValues { (_, latestRevisions) ->
                val amountsBySubledgerDirection = latestRevisions.associate { it.subledgerDirection to it.amount }
                SubledgerBalanceDto(
                    payable = amountsBySubledgerDirection[ContactSubledgerDirection.PAYABLE] ?: BigDecimal.ZERO,
                    receivable = amountsBySubledgerDirection[ContactSubledgerDirection.RECEIVABLE] ?: BigDecimal.ZERO
                )
            }
    }
}
