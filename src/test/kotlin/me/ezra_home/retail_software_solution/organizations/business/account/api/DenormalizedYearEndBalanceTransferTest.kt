package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountBalance
import me.ezra_home.retail_software_solution.organizations.business.account.AccountEntity
import me.ezra_home.retail_software_solution.organizations.business.account.AccountRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import java.lang.reflect.Proxy
import java.math.BigDecimal

class DenormalizedYearEndBalanceTransferTest {

    private val events = mutableListOf<String>()
    private val incrementsByAccountCode = mutableMapOf<String, BigDecimal>()
    private val balancesBySystemAccount = mapOf(
        SystemAccount.GROSS_SALES to BigDecimal("100.00"),
        SystemAccount.SALES_DISCOUNTS to BigDecimal("10.00"),
        SystemAccount.TAX_EXPENSE to BigDecimal("20.00")
    )

    private val accountStructureLock = mock(AccountStructureLock::class.java).also { lock ->
        doAnswer { events.add("lock") }.`when`(lock).acquire(anyCollection())
    }

    private val accountRepository = Proxy.newProxyInstance(
        AccountRepository::class.java.classLoader,
        arrayOf(AccountRepository::class.java)
    ) { _, method, arguments ->
        when (method.name) {
            "findAll" -> SystemAccount.entries.map { systemAccount ->
                AccountEntity(
                    code = systemAccount.code,
                    name = systemAccount.accountName,
                    accountType = systemAccount.type,
                    currencyCode = "USD",
                    accountIsActive = true,
                    accountIsSystemMaintained = true,
                    parentAccountCode = systemAccount.parent?.code
                )
            }
            "findBalancesByCodes" -> {
                events.add("readBalances")
                @Suppress("UNCHECKED_CAST")
                (arguments[0] as Set<String>).map { requestedCode ->
                    object : AccountBalance {
                        override val code = requestedCode
                        override val currentBalance = balancesBySystemAccount.entries
                            .firstOrNull { it.key.code == requestedCode }?.value ?: BigDecimal.ZERO
                    }
                }
            }
            "incrementBalance" -> {
                incrementsByAccountCode.merge(arguments[0] as String, arguments[1] as BigDecimal, BigDecimal::add)
                null
            }
            else -> null
        }
    } as AccountRepository

    @Test
    fun `locks the closing accounts before reading balances and closes net income into retained earnings`() {
        DenormalizedYearEndBalanceTransfer(accountRepository, accountStructureLock).applyYearEndBalanceTransfer()

        assertEquals(listOf("lock", "readBalances"), events)
        assertEquals(0, BigDecimal("-100.00").compareTo(incrementsByAccountCode.getValue(SystemAccount.GROSS_SALES.code)))
        assertEquals(0, BigDecimal("-10.00").compareTo(incrementsByAccountCode.getValue(SystemAccount.SALES_DISCOUNTS.code)))
        assertEquals(0, BigDecimal("-20.00").compareTo(incrementsByAccountCode.getValue(SystemAccount.TAX_EXPENSE.code)))
        // revenue 100 − contra 10 − expense 20
        assertEquals(0, BigDecimal("70.00").compareTo(incrementsByAccountCode.getValue(SystemAccount.RETAINED_EARNINGS.code)))
    }
}
