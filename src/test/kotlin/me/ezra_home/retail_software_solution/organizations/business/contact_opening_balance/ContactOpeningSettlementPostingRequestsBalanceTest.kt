package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountPosting
import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

/** A wrong side or column here silently misstates trade receivables, trade payables or the contact's running balance. */
class ContactOpeningSettlementPostingRequestsBalanceTest {

    private val settlementDate = LocalDate.of(2026, 11, 5)
    private val voidDate = LocalDate.of(2026, 11, 9)
    private val postedRequests = mutableListOf<LedgerPostingRequest>()
    private val recordingAccountService = RecordingAccountService()

    @Test
    fun `receiving from a contact raises the payment account, lowers trade receivables and raises the payable column`() {
        post(recorded(ContactSubledgerDirection.RECEIVABLE))

        assertEquals(0, BigDecimal("40.0000").compareTo(recordingAccountService.balance(SystemAccount.CASH)))
        assertEquals(0, BigDecimal("-40.0000").compareTo(recordingAccountService.balance(SystemAccount.TRADE_RECEIVABLES)))
        assertSubledger(payable = "40.0000", receivable = "0")
    }

    @Test
    fun `paying a contact lowers the payment account, lowers trade payables and raises the receivable column`() {
        post(recorded(ContactSubledgerDirection.PAYABLE))

        assertEquals(0, BigDecimal("-40.0000").compareTo(recordingAccountService.balance(SystemAccount.CASH)))
        assertEquals(0, BigDecimal("-40.0000").compareTo(recordingAccountService.balance(SystemAccount.TRADE_PAYABLES)))
        assertSubledger(payable = "0", receivable = "40.0000")
    }

    @Test
    fun `voiding either direction restores every balance and moves the subledger columns back by the same amount`() {
        listOf(ContactSubledgerDirection.RECEIVABLE, ContactSubledgerDirection.PAYABLE).forEach { subledgerDirection ->
            post(recorded(subledgerDirection))
            post(voided(subledgerDirection))
        }

        listOf(SystemAccount.CASH, SystemAccount.TRADE_RECEIVABLES, SystemAccount.TRADE_PAYABLES).forEach { systemAccount ->
            assertEquals(0, BigDecimal.ZERO.compareTo(recordingAccountService.balance(systemAccount)), systemAccount.name)
        }
        assertSubledger(payable = "80.0000", receivable = "80.0000")
    }

    @Test
    fun `a void shares its original's reference and is a dated reversal with its own source type`() {
        val recordedRequest = recorded(ContactSubledgerDirection.RECEIVABLE)
        val voidedRequest = voided(ContactSubledgerDirection.RECEIVABLE)

        assertEquals(recordedRequest.sourceReferenceNumber, voidedRequest.sourceReferenceNumber)
        assertEquals(LedgerSourceType.CONTACT_OPENING_SETTLEMENT, recordedRequest.sourceType)
        assertEquals(LedgerSourceType.CONTACT_OPENING_SETTLEMENT_VOID, voidedRequest.sourceType)
        assertEquals(settlementDate, recordedRequest.postingDate)
        assertEquals(voidDate, voidedRequest.postingDate)
        assertEquals(listOf(false, true), listOf(recordedRequest.reversal, voidedRequest.reversal))
    }

    private fun settlementEntity(subledgerDirection: ContactSubledgerDirection) = ContactOpeningSettlementEntity(
        contactReferenceNumber = "CON000001",
        subledgerDirection = subledgerDirection,
        paymentAccountCode = SystemAccount.CASH.code,
        amount = BigDecimal("40.0000"),
        settlementDate = settlementDate,
        note = null
    ).also { it.referenceNumber = "CSET000001" }

    private fun recorded(subledgerDirection: ContactSubledgerDirection) =
        ContactOpeningSettlementPostingRequests.recorded(settlementEntity(subledgerDirection))

    private fun voided(subledgerDirection: ContactSubledgerDirection) =
        ContactOpeningSettlementPostingRequests.voided(settlementEntity(subledgerDirection), voidDate)

    private fun post(ledgerPostingRequest: LedgerPostingRequest) {
        val accountPostings = ledgerPostingRequest.entries.map { entry ->
            AccountPosting(
                accountCode = entry.accountCode,
                amount = entry.amount.toBigDecimal(),
                entryType = entry.entryType
            )
        }
        val accountService = recordingAccountService.accountService
        if (ledgerPostingRequest.reversal) {
            accountService.assertPostableReversal(accountPostings)
        } else {
            accountService.assertPostable(accountPostings)
        }
        accountService.patchBalances(accountPostings)
        postedRequests.add(ledgerPostingRequest)
    }

    private fun assertSubledger(payable: String, receivable: String) {
        val payableTotal = subledgerTotal { entry -> entry.payableAmount.toBigDecimal() }
        val receivableTotal = subledgerTotal { entry -> entry.receivableAmount.toBigDecimal() }
        assertEquals(0, BigDecimal(payable).compareTo(payableTotal), "payable column")
        assertEquals(0, BigDecimal(receivable).compareTo(receivableTotal), "receivable column")
    }

    private fun subledgerTotal(amountOf: (SubledgerEntryRequest) -> BigDecimal): BigDecimal =
        postedRequests.flatMap { ledgerPostingRequest -> ledgerPostingRequest.subledgerEntries }
            .fold(BigDecimal.ZERO) { runningTotal, entry -> runningTotal + amountOf(entry) }
}
