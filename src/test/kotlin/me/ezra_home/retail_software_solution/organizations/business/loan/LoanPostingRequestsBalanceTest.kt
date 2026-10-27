package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountPosting
import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class LoanPostingRequestsBalanceTest {

    private val loanDate = LocalDate.of(2026, 3, 1)
    private val repaymentDate = LocalDate.of(2026, 3, 10)
    private val voidDate = LocalDate.of(2026, 3, 20)
    private val lenderContactReferenceNumber = "CT000001"
    private val assetAccountCode = SystemAccount.CASH.code
    private val loanAmount = BigDecimal("100.0000")
    private val repaymentAmount = BigDecimal("30.0000")

    private val postedRequests = mutableListOf<LedgerPostingRequest>()
    private var recordingAccountService = RecordingAccountService()

    @Test
    fun `a loan raises the asset account and loans payable and the lender's payable column`() {
        post(recordedLoan())

        assertBalances(cash = "100.0000", loansPayable = "100.0000")
        assertSubledger(payable = "100.0000", receivable = "0")
    }

    @Test
    fun `a repayment lowers the asset account and loans payable and raises the receivable column`() {
        post(recordedLoan())
        post(recordedRepayment())

        assertBalances(cash = "70.0000", loansPayable = "70.0000")
        assertSubledger(payable = "100.0000", receivable = "30.0000")
    }

    @Test
    fun `voiding a repayment restores the loan's balances and raises the payable column`() {
        post(recordedLoan())
        post(recordedRepayment())
        post(voidedRepayment())

        assertBalances(cash = "100.0000", loansPayable = "100.0000")
        assertSubledger(payable = "130.0000", receivable = "30.0000")
    }

    @Test
    fun `voiding a loan returns every account to zero and raises the receivable column`() {
        post(recordedLoan())
        post(voidedLoan())

        assertBalances(cash = "0", loansPayable = "0")
        assertSubledger(payable = "100.0000", receivable = "100.0000")
    }

    @Test
    fun `loans payable equals the sum of outstanding loans after a mixed history`() {
        post(recordedLoan())
        post(recordedRepayment())
        post(voidedRepayment())
        post(recordedRepayment())
        post(recordedRepayment())

        val outstanding = loanAmount - repaymentAmount - repaymentAmount
        assertEquals(0, outstanding.compareTo(recordingAccountService.balance(SystemAccount.LOANS_PAYABLE)))
        assertEquals(0, outstanding.compareTo(recordingAccountService.balance(SystemAccount.CASH)))
    }

    @Test
    fun `each posting carries its own source type and date, and a void shares its original's reference`() {
        val loan = recordedLoan()
        val loanVoid = voidedLoan()
        val repayment = recordedRepayment()
        val repaymentVoid = voidedRepayment()
        val ledgerPostingRequests = listOf(loan, loanVoid, repayment, repaymentVoid)

        assertEquals(
            listOf(LedgerSourceType.LOAN, LedgerSourceType.LOAN_VOID, LedgerSourceType.LOAN_REPAYMENT, LedgerSourceType.LOAN_REPAYMENT_VOID),
            ledgerPostingRequests.map { ledgerPostingRequest -> ledgerPostingRequest.sourceType }
        )
        assertEquals(
            listOf(loanDate, voidDate, repaymentDate, voidDate),
            ledgerPostingRequests.map { ledgerPostingRequest -> ledgerPostingRequest.postingDate }
        )
        assertEquals(loan.sourceReferenceNumber, loanVoid.sourceReferenceNumber)
        assertEquals(repayment.sourceReferenceNumber, repaymentVoid.sourceReferenceNumber)
        assertEquals(
            listOf(false, true, false, true),
            ledgerPostingRequests.map { ledgerPostingRequest -> ledgerPostingRequest.reversal }
        )
    }

    @Test
    fun `a loan cannot be recorded onto an inactive asset account but both voids still post to it`() {
        deactivateCash()

        assertThrows(RtsGenericException::class.java) { post(recordedLoan()) }

        post(voidedRepayment())
        post(voidedLoan())
        assertBalances(cash = "-70.0000", loansPayable = "-70.0000")
    }

    private fun deactivateCash() {
        val accountDtos = SystemAccount.entries.map { systemAccount ->
            RecordingAccountService.accountDto(systemAccount, accountIsActive = systemAccount != SystemAccount.CASH)
        }
        recordingAccountService = RecordingAccountService(accountDtos)
    }

    private fun recordedLoan() = LoanPostingRequests.loanRecorded("LOAN000001", assetAccountCode, lenderContactReferenceNumber, loanAmount, loanDate)

    private fun voidedLoan() = LoanPostingRequests.loanVoided("LOAN000001", assetAccountCode, lenderContactReferenceNumber, loanAmount, voidDate)

    private fun recordedRepayment() =
        LoanPostingRequests.repaymentRecorded("LNRP000001", assetAccountCode, lenderContactReferenceNumber, repaymentAmount, repaymentDate)

    private fun voidedRepayment() =
        LoanPostingRequests.repaymentVoided("LNRP000001", assetAccountCode, lenderContactReferenceNumber, repaymentAmount, voidDate)

    // LedgerPostingService checks a reversal with the reversal postability rule and any other posting with the strict one.
    private fun post(ledgerPostingRequest: LedgerPostingRequest) {
        val accountPostings = ledgerPostingRequest.entries.map { entry ->
            AccountPosting(accountCode = entry.accountCode, amount = entry.amount.toBigDecimal(), entryType = entry.entryType)
        }
        val accountService = recordingAccountService.accountService
        if (ledgerPostingRequest.reversal) accountService.assertPostableReversal(accountPostings) else accountService.assertPostable(accountPostings)
        accountService.patchBalances(accountPostings)
        postedRequests.add(ledgerPostingRequest)
    }

    private fun assertBalances(cash: String, loansPayable: String) {
        assertEquals(0, BigDecimal(cash).compareTo(recordingAccountService.balance(SystemAccount.CASH)), "cash")
        assertEquals(0, BigDecimal(loansPayable).compareTo(recordingAccountService.balance(SystemAccount.LOANS_PAYABLE)), "loans payable")
    }

    private fun assertSubledger(payable: String, receivable: String) {
        assertEquals(0, BigDecimal(payable).compareTo(subledgerTotal { entry -> entry.payableAmount.toBigDecimal() }), "running payable")
        assertEquals(0, BigDecimal(receivable).compareTo(subledgerTotal { entry -> entry.receivableAmount.toBigDecimal() }), "running receivable")
    }

    private fun subledgerTotal(amountOf: (SubledgerEntryRequest) -> BigDecimal): BigDecimal =
        postedRequests.flatMap { ledgerPostingRequest -> ledgerPostingRequest.subledgerEntries }
            .fold(BigDecimal.ZERO) { runningTotal, entry -> runningTotal + amountOf(entry) }
}
