package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountPosting
import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

/** Control-account sides and subledgerDirection columns must stay mirrored for every revision direction, or the control accounts drift from the contact totals. */
class ContactOpeningBalancePostingRequestsBalanceTest {

    private val recordingAccountService = RecordingAccountService()

    @Test
    fun `a receivable opening balance debits receivables and credits opening balance equity`() {
        post(revision(ContactSubledgerDirection.RECEIVABLE, "100.00"))

        assertBalance("100.00", SystemAccount.TRADE_RECEIVABLES)
        assertBalance("100.00", SystemAccount.OPENING_BALANCE_EQUITY)
    }

    @Test
    fun `a payable opening balance debits opening balance equity and credits payables`() {
        post(revision(ContactSubledgerDirection.PAYABLE, "80.00"))

        assertBalance("80.00", SystemAccount.TRADE_PAYABLES)
        assertBalance("-80.00", SystemAccount.OPENING_BALANCE_EQUITY)
    }

    @Test
    fun `a downward receivable revision posts the same pair with the sides swapped`() {
        post(revision(ContactSubledgerDirection.RECEIVABLE, "100.00"))
        post(revision(ContactSubledgerDirection.RECEIVABLE, "30.00", balanceReduced = true))

        assertBalance("70.00", SystemAccount.TRADE_RECEIVABLES)
        assertBalance("70.00", SystemAccount.OPENING_BALANCE_EQUITY)
    }

    @Test
    fun `a downward payable revision posts the same pair with the sides swapped`() {
        post(revision(ContactSubledgerDirection.PAYABLE, "80.00"))
        post(revision(ContactSubledgerDirection.PAYABLE, "20.00", balanceReduced = true))

        assertBalance("60.00", SystemAccount.TRADE_PAYABLES)
        assertBalance("-60.00", SystemAccount.OPENING_BALANCE_EQUITY)
    }

    @Test
    fun `every direction and change posts its positive amount in the column that moves net the right way`() {
        assertSubledgerColumns(ContactSubledgerDirection.RECEIVABLE, balanceReduced = false, payable = "0", receivable = "30.00")
        assertSubledgerColumns(ContactSubledgerDirection.RECEIVABLE, balanceReduced = true, payable = "30.00", receivable = "0")
        assertSubledgerColumns(ContactSubledgerDirection.PAYABLE, balanceReduced = false, payable = "30.00", receivable = "0")
        assertSubledgerColumns(ContactSubledgerDirection.PAYABLE, balanceReduced = true, payable = "0", receivable = "30.00")
    }

    private fun assertSubledgerColumns(
        subledgerDirection: ContactSubledgerDirection,
        balanceReduced: Boolean,
        payable: String,
        receivable: String
    ) {
        val subledgerEntry = revision(subledgerDirection, "30.00", balanceReduced).subledgerEntries.single()
        assertEquals(0, BigDecimal(payable).compareTo(subledgerEntry.payableAmount.toBigDecimal()), "$subledgerDirection reduced=$balanceReduced payable")
        assertEquals(0, BigDecimal(receivable).compareTo(subledgerEntry.receivableAmount.toBigDecimal()), "$subledgerDirection reduced=$balanceReduced receivable")
    }

    private fun post(ledgerPostingRequest: LedgerPostingRequest) {
        val accountPostings = ledgerPostingRequest.entries.map {
            AccountPosting(accountCode = it.accountCode, amount = it.amount.toBigDecimal(), entryType = it.entryType)
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

    private fun revision(
        subledgerDirection: ContactSubledgerDirection,
        amount: String,
        balanceReduced: Boolean = false
    ): LedgerPostingRequest {
        val signedDelta = BigDecimal(amount).let { if (balanceReduced) it.negate() else it }
        return ContactOpeningBalancePostingRequests.revised(
            ledgerSourceReferenceNumber = "COB000001",
            contactReferenceNumber = "CON000001",
            subledgerDirection = subledgerDirection,
            signedDelta = signedDelta,
            postingDate = LocalDate.of(2026, 1, 1)
        )
    }
}
