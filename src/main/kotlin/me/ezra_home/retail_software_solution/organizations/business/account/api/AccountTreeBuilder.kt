package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.AccountCache
import me.ezra_home.retail_software_solution.organizations.business.account.AccountDto
import me.ezra_home.retail_software_solution.organizations.business.account.AccountType
import me.ezra_home.retail_software_solution.util.ui_models.TreeNode
import org.springframework.stereotype.Component


@Component
@TransactionalOnOrganizationSchema
class AccountTreeBuilder(private val accountCache: AccountCache) {

    fun build(): AccountsTreesForSelection {
        val accounts = accountCache.getAll()
        val accountsByCode = accounts.associateBy { it.code }
        return AccountsTreesForSelection(
            payable = buildTree(accounts, accountsByCode) { isSelectableForTaxPayable(it, accountsByCode) },
            recoverable = buildTree(accounts, accountsByCode) { isSelectableForTaxRecoverable(it, accountsByCode) },
            paymentMethods = buildTree(accounts, accountsByCode) { isSelectableForPaymentMethod(it, accountsByCode) }
        )
    }

    private fun buildTree(
        accounts: List<AccountDto>,
        accountsByCode: Map<String, AccountDto>,
        isSelectable: (AccountDto) -> Boolean
    ): List<TreeNode<String>> {
        val roots = accounts.filter { it.parentAccountCode == null }
        return roots.mapNotNull { buildNode(it, accounts, accountsByCode, isSelectable) }
    }

    private fun buildNode(
        account: AccountDto,
        accounts: List<AccountDto>,
        accountsByCode: Map<String, AccountDto>,
        isSelectable: (AccountDto) -> Boolean
    ): TreeNode<String>? {
        val children = accounts
            .filter { it.parentAccountCode == account.code }
            .mapNotNull { buildNode(it, accounts, accountsByCode, isSelectable) }

        val selectable = isSelectable(account)
        if (!selectable && children.isEmpty()) return null

        return TreeNode(
            key = account.code,
            label = account.label,
            selectable = selectable,
            children = children
        )
    }

    private fun isSelectableForPaymentMethod(account: AccountDto, accountsByCode: Map<String, AccountDto>): Boolean {
        if (SystemAccount.fromCode(account.code) == SystemAccount.CASH) return true
        val parent = accountsByCode[account.parentAccountCode] ?: return false
        return if (parent.accountIsSystemMaintained) {
            SystemAccount.fromCode(parent.code) == SystemAccount.DIGITAL_PAYMENTS
        } else {
            !account.accountIsSystemMaintained && account.accountType == AccountType.ASSET
        }
    }

    private fun isSelectableForTaxPayable(account: AccountDto, accountsByCode: Map<String, AccountDto>): Boolean {
        val parent = accountsByCode[account.parentAccountCode] ?: return false
        return if (parent.accountIsSystemMaintained) {
            SystemAccount.fromCode(parent.code) == SystemAccount.TAX_PAYABLE
        } else {
            !account.accountIsSystemMaintained && account.accountType == AccountType.LIABILITY
        }
    }

    private fun isSelectableForTaxRecoverable(account: AccountDto, accountsByCode: Map<String, AccountDto>): Boolean {
        val parent = accountsByCode[account.parentAccountCode] ?: return false
        return if (parent.accountIsSystemMaintained) {
            SystemAccount.fromCode(parent.code) == SystemAccount.TAX_RECOVERABLE
        } else {
            !account.accountIsSystemMaintained && account.accountType == AccountType.ASSET
        }
    }
}
