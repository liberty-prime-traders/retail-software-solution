package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountCache
import me.ezra_home.retail_software_solution.organizations.business.account.AccountRepository
import me.ezra_home.retail_software_solution.organizations.business.account.AccountResponseBuilder
import me.ezra_home.retail_software_solution.organizations.business.account.ChildAccountCreator
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

// A fresh read taken before the lock would let a concurrent toggle or rename be overwritten by stale state.
class AccountServiceLockOrderTest {

    private val userAccountDto = RecordingAccountService.accountDto(SystemAccount.CASH, accountIsActive = true)
        .copy(accountIsSystemMaintained = false)
    private val accountCache = mock(AccountCache::class.java).also { accountCache ->
        `when`(accountCache.getAll()).thenReturn(listOf(userAccountDto))
        `when`(accountCache.getAllFresh()).thenReturn(listOf(userAccountDto))
    }
    private val accountStructureLock = mock(AccountStructureLock::class.java)
    private val accountService = AccountService(
        accountCache,
        mock(AccountRepository::class.java),
        mock(AccountResponseBuilder::class.java),
        mock(ChildAccountCreator::class.java),
        accountStructureLock
    )

    @Test
    fun `toggleActive takes the account lock before reading fresh state`() {
        accountService.toggleActive(userAccountDto.id, setActive = false)

        val lockThenFreshRead = inOrder(accountStructureLock, accountCache)
        lockThenFreshRead.verify(accountStructureLock).acquire(userAccountDto.code)
        lockThenFreshRead.verify(accountCache).getAllFresh()
    }
}
