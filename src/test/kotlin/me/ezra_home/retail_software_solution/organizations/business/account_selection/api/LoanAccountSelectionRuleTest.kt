package me.ezra_home.retail_software_solution.organizations.business.account_selection.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountSelectionCandidate
import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// LOAN must offer exactly the accounts PAYMENT_METHOD offers, without needing a payment method row.
class LoanAccountSelectionRuleTest {

    private val candidatesByCode = AccountSelectionCandidate
        .fromAccounts(SystemAccount.entries.map { RecordingAccountService.accountDto(it, accountIsActive = true) })
        .associateBy { it.code }

    @Test
    fun `cash is selectable`() {
        assertTrue(AccountSelectionRule.LOAN.isSelectable(candidatesByCode.getValue(SystemAccount.CASH.code)))
    }

    @Test
    fun `parents, other system assets, the liability the loan credits and non-asset accounts are rejected`() {
        listOf(
            SystemAccount.CURRENT_ASSETS, SystemAccount.DIGITAL_PAYMENTS, SystemAccount.INVENTORY, SystemAccount.LOANS_PAYABLE,
            SystemAccount.OWNERS_CAPITAL, SystemAccount.RENT_EXPENSE
        ).forEach { systemAccount ->
            assertFalse(AccountSelectionRule.LOAN.isSelectable(candidatesByCode.getValue(systemAccount.code)), "${systemAccount.name} must not be selectable")
        }
    }

    @Test
    fun `selection agrees with the payment method rule for every system account`() {
        candidatesByCode.values.forEach { candidate ->
            assertEquals(AccountSelectionRule.PAYMENT_METHOD.isSelectable(candidate), AccountSelectionRule.LOAN.isSelectable(candidate), candidate.code)
        }
    }
}
