package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountEntity
import me.ezra_home.retail_software_solution.organizations.business.account.AccountRepository
import me.ezra_home.retail_software_solution.organizations.business.account.AccountType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant

@Service
class DenormalizedYearEndBalanceTransfer(
    private val accountRepository: AccountRepository,
    private val accountStructureLock: AccountStructureLock
) {

    // TODO: Replace with proper year-end closing ledger entries — a ledger_entry_group with
    // source_type = YEAR_END_CLOSE zeroing out revenue/expense accounts into Retained Earnings
    // via double-entry entries, instead of updating running balances directly.
    fun applyYearEndBalanceTransfer() {
        val accounts = accountRepository.findAll()
        val retainedEarnings = accounts.firstOrNull { it.code == SystemAccount.RETAINED_EARNINGS.code }
            ?: throw RtsGenericException("Retained Earnings account not found in organization.")

        val closingAccountsByCode = accounts.filter { it.accountType.isClosingType() }.associateBy { it.code }
        if (closingAccountsByCode.isEmpty()) return

        accountStructureLock.acquire(closingAccountsByCode.keys + retainedEarnings.code)

        // The entities above were loaded before the lock wait, so their balances may predate a posting
        // that just committed; the projection query reads straight from the database.
        val currentBalancesByCode = accountRepository.findBalancesByCodes(closingAccountsByCode.keys)
            .associate { it.code to it.currentBalance }
        val closingAccounts = closingAccountsByCode.values.filter { currentBalancesByCode.getValue(it.code).signum() != 0 }
        if (closingAccounts.isEmpty()) return

        val revenueNet = netBalance(closingAccounts, currentBalancesByCode, AccountType.REVENUE, AccountType.REVENUE_CONTRA)
        val expenseNet = netBalance(closingAccounts, currentBalancesByCode, AccountType.EXPENSE, AccountType.EXPENSE_CONTRA)
        val netIncome = revenueNet - expenseNet

        val balanceUpdatedAt = Instant.now()
        closingAccounts.forEach { closingAccount ->
            accountRepository.incrementBalance(closingAccount.code, currentBalancesByCode.getValue(closingAccount.code).negate(), balanceUpdatedAt)
        }
        accountRepository.incrementBalance(retainedEarnings.code, netIncome, balanceUpdatedAt)
    }

    private fun netBalance(
        closingAccounts: List<AccountEntity>,
        currentBalancesByCode: Map<String, BigDecimal>,
        accountType: AccountType,
        contraAccountType: AccountType
    ): BigDecimal = closingAccounts
        .filter { it.accountType == accountType || it.accountType == contraAccountType }
        .fold(BigDecimal.ZERO) { runningTotal, closingAccount ->
            val currentBalance = currentBalancesByCode.getValue(closingAccount.code)
            if (closingAccount.accountType == accountType) runningTotal + currentBalance else runningTotal - currentBalance
        }
}
