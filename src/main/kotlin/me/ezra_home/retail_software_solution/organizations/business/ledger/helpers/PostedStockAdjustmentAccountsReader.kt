package me.ezra_home.retail_software_solution.organizations.business.ledger.helpers

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

data class PostedStockAdjustmentAccounts(val debitAccountCode: String, val creditAccountCode: String)

@Component
@TransactionalOnOrganizationSchema(readOnly = true)
class PostedStockAdjustmentAccountsReader(
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    private val ledgerEntryRepository: LedgerEntryRepository
) {

    // A group with no entries is a cost-zero adjustment (absent from the result); no group means it has not posted yet.
    fun getByAdjustmentReferenceNumber(
        adjustmentReferenceNumbers: Set<String>
    ): Map<String, PostedStockAdjustmentAccounts> {
        val groups = ledgerEntryGroupRepository.findBySourceReferenceNumberInAndSourceTypeAndSourceLocationId(
            adjustmentReferenceNumbers, LedgerSourceType.STOCK_ADJUSTMENT, SessionContextProvider.getLocationId()
        )
        val notPosted = adjustmentReferenceNumbers - groups.map { it.sourceReferenceNumber }.toSet()
        if (notPosted.isNotEmpty()) {
            throw RtsGenericException(
                "Cannot restate $notPosted: their ${LedgerSourceType.STOCK_ADJUSTMENT.name} posting does not exist"
            )
        }
        val sourceReferenceNumberByGroup = groups.associate { it.requiredReference() to it.sourceReferenceNumber }
        return ledgerEntryRepository.findByGroupReferenceNumberIn(sourceReferenceNumberByGroup.keys)
            .groupBy { sourceReferenceNumberByGroup.getValue(it.groupReferenceNumber) }
            .mapValues { (_, entries) ->
                PostedStockAdjustmentAccounts(
                    debitAccountCode = entries.first { it.entryType == EntryType.DEBIT }.accountCode,
                    creditAccountCode = entries.first { it.entryType == EntryType.CREDIT }.accountCode
                )
            }
    }
}
