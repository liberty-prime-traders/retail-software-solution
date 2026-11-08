package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

data class PostedStockAdjustmentAccounts(val debitAccountCode: String, val creditAccountCode: String)

@Component
@TransactionalOnOrganizationSchema(readOnly = true)
class PostedStockAdjustmentAccountsReader(
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    private val ledgerEntryRepository: LedgerEntryRepository
) {

    // A group with no entries is a cost-zero adjustment, which has no accounts to return.
    // No group means the original has not posted yet, which a posting must wait for.
    fun getByAdjustmentReferenceNumber(
        adjustmentReferenceNumbers: Set<String>
    ): Map<String, PostedStockAdjustmentAccounts> {
        val groups = groupsOf(adjustmentReferenceNumbers)
        val notPosted = adjustmentReferenceNumbers - groups.map { it.sourceReferenceNumber }.toSet()
        if (notPosted.isNotEmpty()) {
            throw RtsGenericException(
                "Cannot restate $notPosted: their ${LedgerSourceType.STOCK_ADJUSTMENT.name} posting does not exist"
            )
        }
        return accountsOf(groups)
    }

    // For a preview, which has nothing to wait for: an adjustment that has not posted is simply absent.
    fun findByAdjustmentReferenceNumber(
        adjustmentReferenceNumbers: Set<String>
    ): Map<String, PostedStockAdjustmentAccounts> = accountsOf(groupsOf(adjustmentReferenceNumbers))

    private fun groupsOf(adjustmentReferenceNumbers: Set<String>) =
        ledgerEntryGroupRepository.findBySourceReferenceNumberInAndSourceTypeAndSourceLocationId(
            adjustmentReferenceNumbers, LedgerSourceType.STOCK_ADJUSTMENT, SessionContextProvider.getLocationId()
        )

    private fun accountsOf(groups: List<LedgerEntryGroupEntity>): Map<String, PostedStockAdjustmentAccounts> {
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
