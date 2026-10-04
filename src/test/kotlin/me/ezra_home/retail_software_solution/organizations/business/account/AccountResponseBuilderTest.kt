package me.ezra_home.retail_software_solution.organizations.business.account

import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.opening_balance.api.OpeningBalanceService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anySet
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal

class AccountResponseBuilderTest {

    private val accountRepository = mock(AccountRepository::class.java)
    private val openingBalanceService = mock(OpeningBalanceService::class.java)
    private val accountResponseBuilder = AccountResponseBuilder(accountRepository, openingBalanceService)

    @Test
    fun `contra children are subtracted from their parent in the rolled up balance`() {
        stubLeafBalances(
            SystemAccount.GROSS_SALES to "100.00",
            SystemAccount.SALES_DISCOUNTS to "10.00",
            SystemAccount.SALES_RETURNS to "5.00",
            SystemAccount.TRADE_RECEIVABLES to "90.00",
            SystemAccount.ALLOWANCE_FOR_DOUBTFUL_ACCOUNTS to "10.00"
        )

        val responsesByCode = accountResponseBuilder.buildResponse(RecordingAccountService.systemAccountDtos()).associateBy { it.code }

        assertEquals(0, BigDecimal("85.00").compareTo(responsesByCode.getValue(SystemAccount.SALES_REVENUE.code).currentBalance))
        assertEquals(0, BigDecimal("85.00").compareTo(responsesByCode.getValue(SystemAccount.REVENUE.code).currentBalance))
        assertEquals(0, BigDecimal("80.00").compareTo(responsesByCode.getValue(SystemAccount.ACCOUNTS_RECEIVABLE.code).currentBalance))
    }

    @Test
    fun `opening balances roll up the same way`() {
        stubLeafBalances()
        `when`(openingBalanceService.getAmountsByAccountCodes(anySet())).thenReturn(
            mapOf(
                SystemAccount.GROSS_SALES.code to BigDecimal("100.00"),
                SystemAccount.SALES_DISCOUNTS.code to BigDecimal("10.00")
            )
        )

        val responsesByCode = accountResponseBuilder.buildResponse(RecordingAccountService.systemAccountDtos()).associateBy { it.code }

        assertEquals(0, BigDecimal("90.00").compareTo(responsesByCode.getValue(SystemAccount.SALES_REVENUE.code).openingBalance))
    }

    private fun stubLeafBalances(vararg balancesBySystemAccount: Pair<SystemAccount, String>) {
        val balances = balancesBySystemAccount.map { (systemAccount, amount) ->
            object : AccountBalance {
                override val code = systemAccount.code
                override val currentBalance = BigDecimal(amount)
            }
        }
        `when`(accountRepository.findBalancesByCodes(anySet())).thenReturn(balances)
        `when`(openingBalanceService.getAmountsByAccountCodes(anySet())).thenReturn(emptyMap())
    }
}
