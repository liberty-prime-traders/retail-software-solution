package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ContactOpeningBalanceUpsertedEvent
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountPosting
import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.ContactOpeningBalanceAccountingProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.LedgerPostingGate
import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Control-account sides and subledgerDirection columns must stay mirrored for every revision direction, or the control accounts drift from the contact totals. */
@WithDefaultOrganizationSession
class ContactOpeningBalancePostingTest {

    private val recordingAccountService = RecordingAccountService()
    private val contactOpeningBalanceProcessor = ContactOpeningBalanceAccountingProcessor(
        LedgerPostingGate(mock(LedgerEntryGroupRepository::class.java)),
        mock(LedgerPostingService::class.java)
    )

    @Test
    fun `a receivable opening balance debits receivables and credits opening balance equity`() {
        post(event(ContactSubledgerDirection.RECEIVABLE, "100.00"))

        assertBalance("100.00", SystemAccount.TRADE_RECEIVABLES)
        assertBalance("100.00", SystemAccount.OPENING_BALANCE_EQUITY)
    }

    @Test
    fun `a payable opening balance debits opening balance equity and credits payables`() {
        post(event(ContactSubledgerDirection.PAYABLE, "80.00"))

        assertBalance("80.00", SystemAccount.TRADE_PAYABLES)
        assertBalance("-80.00", SystemAccount.OPENING_BALANCE_EQUITY)
    }

    @Test
    fun `a downward receivable revision posts the same pair with the sides swapped`() {
        post(event(ContactSubledgerDirection.RECEIVABLE, "100.00"))
        post(event(ContactSubledgerDirection.RECEIVABLE, "30.00", balanceReduced = true))

        assertBalance("70.00", SystemAccount.TRADE_RECEIVABLES)
        assertBalance("70.00", SystemAccount.OPENING_BALANCE_EQUITY)
    }

    @Test
    fun `a downward payable revision posts the same pair with the sides swapped`() {
        post(event(ContactSubledgerDirection.PAYABLE, "80.00"))
        post(event(ContactSubledgerDirection.PAYABLE, "20.00", balanceReduced = true))

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
        val subledgerEntry = contactOpeningBalanceProcessor
            .prepareLedgerRequest(event(subledgerDirection, "30.00", balanceReduced))
            .subledgerEntries.single()
        assertEquals(0, BigDecimal(payable).compareTo(subledgerEntry.payableAmount), "$subledgerDirection reduced=$balanceReduced payable")
        assertEquals(0, BigDecimal(receivable).compareTo(subledgerEntry.receivableAmount), "$subledgerDirection reduced=$balanceReduced receivable")
    }

    private fun post(event: ContactOpeningBalanceUpsertedEvent) {
        val ledgerPostingRequest: LedgerPostingRequest = contactOpeningBalanceProcessor.prepareLedgerRequest(event)
        LedgerEntriesValidator.validate(ledgerPostingRequest.entries)
        val accountPostings = ledgerPostingRequest.entries.map {
            AccountPosting(accountCode = it.accountCode, amount = it.amount, entryType = it.entryType)
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

    private fun event(subledgerDirection: ContactSubledgerDirection, amount: String, balanceReduced: Boolean = false) = ContactOpeningBalanceUpsertedEvent(
        eventId = UUID.randomUUID(),
        sourceContext = EventSourceContext.OrgLevel(orgSchema = "org-a"),
        timestamp = Instant.now(),
        correlationId = null,
        contactOpeningBalanceId = UUID.randomUUID(),
        ledgerSourceReferenceNumber = "COB000001",
        contactReferenceNumber = "CON000001",
        subledgerDirection = subledgerDirection,
        amount = BigDecimal(amount),
        balanceReduced = balanceReduced,
        postingDate = LocalDate.of(2026, 1, 1)
    )
}
