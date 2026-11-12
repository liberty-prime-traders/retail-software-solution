package me.ezra_home.retail_software_solution.organizations.business.opening_balance

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountPosting
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

/** The account takes the side of its normal balance and opening balance equity the other, or the books stop balancing. */
class OpeningBalancePostingRequestsBalanceTest {

    private val recordingAccountService = RecordingAccountService()

    @Test
    fun `a debit opening balance on a debit normal account raises both sides`() {
        post(revision(SystemAccount.CASH, EntryType.DEBIT, "50.00"))

        assertBalance("50.00", SystemAccount.CASH)
        assertBalance("50.00", SystemAccount.OPENING_BALANCE_EQUITY)
    }

    @Test
    fun `a credit opening balance on a credit normal account raises it and draws equity down`() {
        post(revision(SystemAccount.TRADE_PAYABLES, EntryType.CREDIT, "30.00"))

        assertBalance("30.00", SystemAccount.TRADE_PAYABLES)
        assertBalance("-30.00", SystemAccount.OPENING_BALANCE_EQUITY)
    }

    @Test
    fun `lowering a debit opening balance posts the opposite pair for the change`() {
        post(revision(SystemAccount.CASH, EntryType.DEBIT, "50.00"))
        post(revision(SystemAccount.CASH, EntryType.DEBIT, "-20.00"))

        assertBalance("30.00", SystemAccount.CASH)
        assertBalance("30.00", SystemAccount.OPENING_BALANCE_EQUITY)
    }

    private fun revision(systemAccount: SystemAccount, normalBalanceEntryType: EntryType, signedDelta: String) =
        OpeningBalancePostingRequests.revised(
            ledgerSourceReferenceNumber = "OB000001",
            accountCode = systemAccount.code,
            normalBalanceEntryType = normalBalanceEntryType,
            signedDelta = BigDecimal(signedDelta),
            postingDate = LocalDate.of(2026, 1, 1)
        )

    private fun post(ledgerPostingRequest: LedgerPostingRequest) {
        val accountPostings = ledgerPostingRequest.entries.map { entry ->
            AccountPosting(
                accountCode = entry.accountCode,
                amount = entry.amount.toBigDecimal(),
                entryType = entry.entryType
            )
        }
        recordingAccountService.accountService.assertPostable(accountPostings)
        recordingAccountService.accountService.patchBalances(accountPostings)
    }

    private fun assertBalance(expected: String, systemAccount: SystemAccount) {
        assertEquals(
            0,
            BigDecimal(expected).compareTo(recordingAccountService.balance(systemAccount)),
            "${systemAccount.name} expected $expected but was ${recordingAccountService.balance(systemAccount)}"
        )
    }
}
