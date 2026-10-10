package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountDto
import me.ezra_home.retail_software_solution.organizations.business.account.AccountType
import me.ezra_home.retail_software_solution.organizations.business.account_selection.api.AccountSelectionRule
import me.ezra_home.retail_software_solution.organizations.business.account_selection.api.AccountTreeBuilder
import me.ezra_home.retail_software_solution.util.ui_models.TreeNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class AccountSelectionRuleTest {

    private val systemAccountDtos = SystemAccount.entries.map {
        accountDto(it.code, it.type, it.parent?.code, accountIsSystemMaintained = true)
    }

    @Test
    fun `payment methods accept cash, digital payment children and org asset children`() {
        val digitalPaymentsChild = accountDto("001.001.005.001", AccountType.ASSET, SystemAccount.DIGITAL_PAYMENTS.code)
        val orgBank = accountDto("001.003", AccountType.ASSET, null)
        val orgBankChild = accountDto("001.003.001", AccountType.ASSET, orgBank.code)
        val candidates = candidatesOf(digitalPaymentsChild, orgBank, orgBankChild)

        assertSelectable(AccountSelectionRule.PAYMENT_METHOD, candidates, SystemAccount.CASH.code)
        assertSelectable(AccountSelectionRule.PAYMENT_METHOD, candidates, digitalPaymentsChild.code)
        assertSelectable(AccountSelectionRule.PAYMENT_METHOD, candidates, orgBankChild.code)
    }

    @Test
    fun `payment methods reject non-asset, parentless and foreign system-parent accounts`() {
        val orgRootAsset = accountDto("001.003", AccountType.ASSET, null)
        val taxRecoverableChild = accountDto("001.001.004.001", AccountType.ASSET, SystemAccount.TAX_RECOVERABLE.code)
        val candidates = candidatesOf(orgRootAsset, taxRecoverableChild)

        assertRejected(AccountSelectionRule.PAYMENT_METHOD, candidates, SystemAccount.ALLOWANCE_FOR_DOUBTFUL_ACCOUNTS.code)
        assertRejected(AccountSelectionRule.PAYMENT_METHOD, candidates, orgRootAsset.code)
        assertRejected(AccountSelectionRule.PAYMENT_METHOD, candidates, taxRecoverableChild.code)
    }

    @Test
    fun `tax accounts accept children of their system parent and org-defined accounts of the right type`() {
        val payableChild = accountDto("002.002.001", AccountType.LIABILITY, SystemAccount.TAX_PAYABLE.code)
        val orgRootLiability = accountDto("002.004", AccountType.LIABILITY, null)
        val recoverableChild = accountDto("001.001.004.001", AccountType.ASSET, SystemAccount.TAX_RECOVERABLE.code)
        val candidates = candidatesOf(payableChild, orgRootLiability, recoverableChild)

        assertSelectable(AccountSelectionRule.TAX_PAYABLE, candidates, payableChild.code)
        assertSelectable(AccountSelectionRule.TAX_PAYABLE, candidates, orgRootLiability.code)
        assertSelectable(AccountSelectionRule.TAX_RECOVERABLE, candidates, recoverableChild.code)
    }

    @Test
    fun `tax accounts reject the system parent itself and accounts of the wrong type`() {
        val candidates = candidatesOf()

        assertRejected(AccountSelectionRule.TAX_PAYABLE, candidates, SystemAccount.TAX_PAYABLE.code)
        assertRejected(AccountSelectionRule.TAX_PAYABLE, candidates, SystemAccount.CASH.code)
        assertRejected(AccountSelectionRule.TAX_RECOVERABLE, candidates, SystemAccount.TAX_RECOVERABLE.code)
        assertRejected(AccountSelectionRule.TAX_RECOVERABLE, candidates, SystemAccount.TAX_PAYABLE.code)
    }

    @Test
    fun `org-defined tax accounts must be exactly liability for payable and asset for recoverable`() {
        val orgAsset = accountDto("001.003", AccountType.ASSET, null)
        val orgLiability = accountDto("002.004", AccountType.LIABILITY, null)
        val orgContraLiability = accountDto("002.005", AccountType.LIABILITY_CONTRA, null)
        val orgContraAsset = accountDto("001.004", AccountType.ASSET_CONTRA, null)
        val orgExpense = accountDto("005.100", AccountType.EXPENSE, null)
        val candidates = candidatesOf(orgAsset, orgLiability, orgContraLiability, orgContraAsset, orgExpense)

        assertRejected(AccountSelectionRule.TAX_PAYABLE, candidates, orgAsset.code)
        assertRejected(AccountSelectionRule.TAX_PAYABLE, candidates, orgContraLiability.code)
        assertRejected(AccountSelectionRule.TAX_PAYABLE, candidates, orgExpense.code)
        assertRejected(AccountSelectionRule.TAX_RECOVERABLE, candidates, orgLiability.code)
        assertRejected(AccountSelectionRule.TAX_RECOVERABLE, candidates, orgContraAsset.code)
        assertRejected(AccountSelectionRule.TAX_RECOVERABLE, candidates, orgExpense.code)
    }

    @Test
    fun `every rule rejects an account that has child accounts`() {
        val orgBank = accountDto("001.003", AccountType.ASSET, null)
        val orgBankChild = accountDto("001.003.001", AccountType.ASSET, orgBank.code)
        val orgLiability = accountDto("002.004", AccountType.LIABILITY, null)
        val orgLiabilityChild = accountDto("002.004.001", AccountType.LIABILITY, orgLiability.code)
        val orgExpense = accountDto("005.100", AccountType.EXPENSE, null)
        val orgExpenseChild = accountDto("005.100.001", AccountType.EXPENSE, orgExpense.code)
        val candidates = candidatesOf(orgBank, orgBankChild, orgLiability, orgLiabilityChild, orgExpense, orgExpenseChild)

        assertRejected(AccountSelectionRule.PAYMENT_METHOD, candidates, orgBank.code)
        assertRejected(AccountSelectionRule.TAX_RECOVERABLE, candidates, orgBank.code)
        assertRejected(AccountSelectionRule.TAX_PAYABLE, candidates, orgLiability.code)
        assertRejected(AccountSelectionRule.EXPENSE_TYPE, candidates, orgExpense.code)
        assertSelectable(AccountSelectionRule.TAX_PAYABLE, candidates, orgLiabilityChild.code)
        assertSelectable(AccountSelectionRule.EXPENSE_TYPE, candidates, orgExpenseChild.code)
    }

    @Test
    fun `expense types accept leaf expense accounts that no flow posts to directly`() {
        val orgExpense = accountDto("005.100", AccountType.EXPENSE, null)
        val candidates = candidatesOf(orgExpense)

        assertSelectable(AccountSelectionRule.EXPENSE_TYPE, candidates, SystemAccount.RENT_EXPENSE.code)
        assertSelectable(AccountSelectionRule.EXPENSE_TYPE, candidates, orgExpense.code)
    }

    @Test
    fun `expense types reject reserved, non-leaf and non-expense accounts`() {
        val candidates = candidatesOf()

        listOf(
            SystemAccount.COST_OF_GOODS_SOLD, SystemAccount.WAGES_EXPENSE, SystemAccount.INBOUND_FREIGHT,
            SystemAccount.OUTBOUND_FREIGHT, SystemAccount.SHRINKAGE_AND_LOSSES, SystemAccount.BAD_DEBT_EXPENSE,
            SystemAccount.TAX_EXPENSE
        ).forEach { assertRejected(AccountSelectionRule.EXPENSE_TYPE, candidates, it.code) }
        assertRejected(AccountSelectionRule.EXPENSE_TYPE, candidates, SystemAccount.EXPENSES.code)
        assertRejected(AccountSelectionRule.EXPENSE_TYPE, candidates, SystemAccount.CASH.code)
    }

    @Test
    fun `the tree omits inactive accounts and keeps ancestors of selectable ones`() {
        val inactiveExpense = accountDto("005.100", AccountType.EXPENSE, SystemAccount.EXPENSES.code, accountIsActive = false)
        val candidates = candidatesOf(inactiveExpense)

        val expenseTree = AccountTreeBuilder().build(candidates, AccountSelectionRule.EXPENSE_TYPE)

        assertEquals(listOf(SystemAccount.EXPENSES.code), expenseTree.map { it.key })
        val expenseChildren = expenseTree.single().children
        assertFalse(expenseTree.single().selectable)
        assertNull(expenseChildren.find { it.key == inactiveExpense.code })
        assertTrue(expenseChildren.any { it.key == SystemAccount.RENT_EXPENSE.code && it.selectable })
        assertNull(findNode(expenseTree, SystemAccount.WAGES_EXPENSE.code))
    }

    private fun candidatesOf(vararg orgAccountDtos: AccountDto): List<AccountSelectionCandidate> =
        AccountSelectionCandidate.fromAccounts(systemAccountDtos + orgAccountDtos)

    private fun assertSelectable(rule: AccountSelectionRule, candidates: List<AccountSelectionCandidate>, accountCode: String) {
        val reason = rule.rejectionReason(candidates.single { it.code == accountCode })
        assertNull(reason, "$accountCode should be selectable for $rule but was rejected: $reason")
    }

    private fun assertRejected(rule: AccountSelectionRule, candidates: List<AccountSelectionCandidate>, accountCode: String) {
        assertNotNull(rule.rejectionReason(candidates.single { it.code == accountCode }), "$accountCode should be rejected for $rule")
    }

    private fun findNode(nodes: List<TreeNode<String>>, key: String): TreeNode<String>? =
        nodes.firstNotNullOfOrNull { if (it.key == key) it else findNode(it.children, key) }

    private fun accountDto(
        code: String,
        accountType: AccountType,
        parentAccountCode: String?,
        accountIsSystemMaintained: Boolean = false,
        accountIsActive: Boolean = true
    ) = AccountDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        code = code,
        name = code,
        accountType = accountType,
        currencyCode = "USD",
        accountIsActive = accountIsActive,
        accountIsSystemMaintained = accountIsSystemMaintained,
        parentAccountCode = parentAccountCode
    )
}
