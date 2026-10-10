package me.ezra_home.retail_software_solution.organizations.business.account_selection.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountSelectionCandidate
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountStructureLock
import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

// A fresh read taken before the lock would let createChild or toggleActive change the account between check and save.
class AccountSelectionServiceTest {

    private val accountDataFetcher = mock(AccountDataFetcher::class.java)
    private val accountStructureLock = mock(AccountStructureLock::class.java)
    private val accountSelectionService = AccountSelectionService(accountDataFetcher, AccountTreeBuilder(), accountStructureLock)

    @Test
    fun `requireSelectable takes the account lock before reading fresh candidates`() {
        `when`(accountDataFetcher.getFreshSelectionCandidates()).thenReturn(candidatesOf(activeAccounts = true))

        accountSelectionService.requireSelectable(AccountSelectionRule.EXPENSE_TYPE, SystemAccount.RENT_EXPENSE.code)

        val lockThenFreshRead = inOrder(accountStructureLock, accountDataFetcher)
        lockThenFreshRead.verify(accountStructureLock).acquire(SystemAccount.RENT_EXPENSE.code)
        lockThenFreshRead.verify(accountDataFetcher).getFreshSelectionCandidates()
    }

    @Test
    fun `requireSelectable refuses unknown and inactive accounts before consulting the rule`() {
        `when`(accountDataFetcher.getFreshSelectionCandidates()).thenReturn(candidatesOf(activeAccounts = false))

        assertThrows(RtsGenericException::class.java) {
            accountSelectionService.requireSelectable(AccountSelectionRule.EXPENSE_TYPE, "999.999")
        }
        assertThrows(RtsGenericException::class.java) {
            accountSelectionService.requireSelectable(AccountSelectionRule.EXPENSE_TYPE, SystemAccount.RENT_EXPENSE.code)
        }
    }

    private fun candidatesOf(activeAccounts: Boolean): List<AccountSelectionCandidate> =
        AccountSelectionCandidate.fromAccounts(
            SystemAccount.entries.map { RecordingAccountService.accountDto(it, accountIsActive = activeAccounts) }
        )
}
