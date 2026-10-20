package me.ezra_home.retail_software_solution.organizations.business.account_selection.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountSelectionCandidate
import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FundMovementAccountSelectionRuleTest {

    private val candidatesByCode = AccountSelectionCandidate
        .fromAccounts(SystemAccount.entries.map { RecordingAccountService.accountDto(it, accountIsActive = true) })
        .associateBy { it.code }

    @Test
    fun `cash is selectable`() {
        assertTrue(AccountSelectionRule.FUND_MOVEMENT.isSelectable(candidatesByCode.getValue(SystemAccount.CASH.code)))
    }

    @Test
    fun `parents, other system assets and non-asset accounts are rejected`() {
        listOf(SystemAccount.CURRENT_ASSETS, SystemAccount.DIGITAL_PAYMENTS, SystemAccount.INVENTORY, SystemAccount.OWNERS_CAPITAL, SystemAccount.RENT_EXPENSE)
            .forEach { systemAccount ->
                assertFalse(
                    AccountSelectionRule.FUND_MOVEMENT.isSelectable(candidatesByCode.getValue(systemAccount.code)),
                    "${systemAccount.name} must not be selectable"
                )
            }
    }

    @Test
    fun `selection agrees with the payment method rule for every system account`() {
        candidatesByCode.values.forEach { candidate ->
            assertEquals(
                AccountSelectionRule.PAYMENT_METHOD.isSelectable(candidate),
                AccountSelectionRule.FUND_MOVEMENT.isSelectable(candidate),
                candidate.code
            )
        }
    }
}
