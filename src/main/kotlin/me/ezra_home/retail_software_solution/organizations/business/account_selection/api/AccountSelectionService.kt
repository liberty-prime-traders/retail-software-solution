package me.ezra_home.retail_software_solution.organizations.business.account_selection.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountStructureLock
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class AccountSelectionService(
    private val accountDataFetcher: AccountDataFetcher,
    private val accountTreeBuilder: AccountTreeBuilder,
    private val accountStructureLock: AccountStructureLock
) {

    fun buildTreesForSelection(): AccountsTreesForSelection {
        val candidates = accountDataFetcher.getSelectionCandidates()
        return AccountsTreesForSelection(
            payable = accountTreeBuilder.build(candidates, AccountSelectionRule.TAX_PAYABLE),
            recoverable = accountTreeBuilder.build(candidates, AccountSelectionRule.TAX_RECOVERABLE),
            paymentMethods = accountTreeBuilder.build(candidates, AccountSelectionRule.PAYMENT_METHOD),
            expenseTypes = accountTreeBuilder.build(candidates, AccountSelectionRule.EXPENSE_TYPE)
        )
    }

    // The lock holds until the caller's transaction ends, so the leaf/active facts checked here cannot change before the
    // caller saves its reference; createChild and toggleActive take the same lock.
    @TransactionalOnOrganizationSchema
    fun requireSelectable(rule: AccountSelectionRule, accountCode: String) {
        accountStructureLock.acquire(accountCode)
        val candidate = accountDataFetcher.getFreshSelectionCandidates().firstOrNull { it.code == accountCode }
            ?: throw RtsGenericException("${rule.accountDescription} not found: $accountCode")
        if (!candidate.accountIsActive) {
            throw RtsGenericException("${rule.accountDescription} is inactive: ${candidate.label}")
        }
        rule.rejectionReason(candidate)?.let { throw RtsGenericException(it) }
    }
}
