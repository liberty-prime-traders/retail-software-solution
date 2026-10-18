package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountCache
import me.ezra_home.retail_software_solution.organizations.business.account.AccountRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AccountDataFetcherLabelsTest {

    private val accountCache = mock(AccountCache::class.java)
    private val accountDataFetcher = AccountDataFetcher(mock(AccountRepository::class.java), accountCache)
    private val accountDtos = RecordingAccountService.systemAccountDtos()

    @Test
    fun `labels are returned only for the requested codes`() {
        `when`(accountCache.getAll()).thenReturn(accountDtos)
        val cashAccountDto = accountDtos.first { it.code == SystemAccount.CASH.code }

        val labelsByCode = accountDataFetcher.getLabelsByCodes(setOf(SystemAccount.CASH.code, "999.999"))

        assertEquals(mapOf(cashAccountDto.code to cashAccountDto.label), labelsByCode)
    }
}
