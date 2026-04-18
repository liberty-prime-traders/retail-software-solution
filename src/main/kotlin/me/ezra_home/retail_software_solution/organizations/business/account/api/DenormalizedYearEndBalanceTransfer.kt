package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountRepository
import me.ezra_home.retail_software_solution.organizations.business.account.AccountType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant

@Service
class DenormalizedYearEndBalanceTransfer(
    private val accountRepository: AccountRepository
) {

    // TODO: Replace with proper year-end closing ledger entries
    // Current implementation updates running balances directly without creating ledger entries
    // Year-end close should create a ledger_entry_group with source_type = YEAR_END_CLOSE
    // zeroing out revenue/expense accounts into Retained Earnings via double-entry entries
    fun applyYearEndBalanceTransfer() {
        val accounts = accountRepository.findAll()
        val retainedEarnings = accounts.firstOrNull { it.code == SystemAccount.RETAINED_EARNINGS.code }
            ?: throw RtsGenericException("Retained Earnings account not found in organization.")

        val closingAccounts = accounts.filter { it.accountType.isClosingType() && it.currentBalance.compareTo(BigDecimal.ZERO) != 0 }

        if (closingAccounts.isEmpty()) return

        val revenueNet = closingAccounts
            .filter { it.accountType == AccountType.REVENUE || it.accountType == AccountType.REVENUE_CONTRA }
            .fold(BigDecimal.ZERO) { acc, acct ->
                if (acct.accountType == AccountType.REVENUE) acc + acct.currentBalance else acc - acct.currentBalance
            }
        val expenseNet = closingAccounts
            .filter { it.accountType == AccountType.EXPENSE }
            .fold(BigDecimal.ZERO) { acc, acct -> acc + acct.currentBalance }
        val netIncome = revenueNet - expenseNet

        closingAccounts.forEach { acct ->
            accountRepository.incrementBalance(acct.code, acct.currentBalance.negate(), Instant.now())
        }
        accountRepository.incrementBalance(retainedEarnings.code, netIncome, Instant.now())
    }
}
