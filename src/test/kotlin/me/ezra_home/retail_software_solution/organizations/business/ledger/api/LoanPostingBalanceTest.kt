package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerBalanceEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerBalanceRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerEntryRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class LoanPostingBalanceTest {

    private val loanDate = LocalDate.of(2026, 3, 1)
    private val repaymentDate = LocalDate.of(2026, 3, 10)
    private val voidDate = LocalDate.of(2026, 3, 20)
    private val lenderContactReferenceNumber = "CT000001"
    private val assetAccountCode = SystemAccount.CASH.code
    private val loanAmount = BigDecimal("100.0000")
    private val repaymentAmount = BigDecimal("30.0000")

    private val subledgerBalanceEntity = SubledgerBalanceEntity(lenderContactReferenceNumber, BigDecimal.ZERO, BigDecimal.ZERO)
    private val savedGroups = mutableListOf<LedgerEntryGroupEntity>()
    private val fiscalPeriodService = mock(FiscalPeriodService::class.java)
    private var recordingAccountService = RecordingAccountService()
    private var ledgerPostingService = buildLedgerPostingService()

    @BeforeEach
    fun setUpOrganizationSession() {
        SessionContextProvider.setSession(SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC")))
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

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

        assertEquals(
            listOf(LedgerSourceType.LOAN, LedgerSourceType.LOAN_VOID, LedgerSourceType.LOAN_REPAYMENT, LedgerSourceType.LOAN_REPAYMENT_VOID),
            listOf(loan, loanVoid, repayment, repaymentVoid).map { it.sourceType }
        )
        assertEquals(listOf(loanDate, voidDate, repaymentDate, voidDate), listOf(loan, loanVoid, repayment, repaymentVoid).map { it.postingDate })
        assertEquals(loan.sourceReferenceNumber, loanVoid.sourceReferenceNumber)
        assertEquals(repayment.sourceReferenceNumber, repaymentVoid.sourceReferenceNumber)
        assertEquals(listOf(false, true, false, true), listOf(loan, loanVoid, repayment, repaymentVoid).map { it.reversal })
    }

    @Test
    fun `a posted loan group has a null source location`() {
        post(recordedLoan())
        post(voidedLoan())

        assertEquals(2, savedGroups.size)
        savedGroups.forEach { assertNull(it.sourceLocationId) }
    }

    @Test
    fun `a loan cannot be recorded onto an inactive asset account but both voids still post to it`() {
        deactivateCash()

        assertThrows(RtsGenericException::class.java) { post(recordedLoan()) }

        post(voidedRepayment())
        post(voidedLoan())
        assertBalances(cash = "-70.0000", loansPayable = "-70.0000")
    }

    @Test
    fun `a posting into a closed fiscal period is rejected`() {
        `when`(fiscalPeriodService.findOpenForDate(loanDate)).thenReturn(null)

        assertThrows(RtsGenericException::class.java) { post(recordedLoan()) }
        assertTrue(savedGroups.isEmpty())
    }

    private fun buildLedgerPostingService(): LedgerPostingService {
        val ledgerEntryGroupRepository = mock(LedgerEntryGroupRepository::class.java)
        `when`(ledgerEntryGroupRepository.save(any(LedgerEntryGroupEntity::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as LedgerEntryGroupEntity).also {
                it.referenceNumber = "LG000001"
                savedGroups.add(it)
            }
        }
        listOf(loanDate, repaymentDate, voidDate).forEach { `when`(fiscalPeriodService.findOpenForDate(it)).thenReturn(UUID.randomUUID()) }
        val subledgerBalanceRepository = mock(SubledgerBalanceRepository::class.java)
        `when`(subledgerBalanceRepository.findByContactReferenceNumberIn(setOf(lenderContactReferenceNumber)))
            .thenReturn(listOf(subledgerBalanceEntity))
        return LedgerPostingService(
            ledgerEntryGroupRepository,
            mock(LedgerEntryRepository::class.java),
            mock(SubledgerEntryRepository::class.java),
            subledgerBalanceRepository,
            mock(AccountsAndLedgerLock::class.java),
            fiscalPeriodService,
            recordingAccountService.accountService
        )
    }

    private fun deactivateCash() {
        val accountDtos = SystemAccount.entries.map {
            RecordingAccountService.accountDto(it, accountIsActive = it != SystemAccount.CASH)
        }
        recordingAccountService = RecordingAccountService(accountDtos)
        ledgerPostingService = buildLedgerPostingService()
    }

    private fun recordedLoan() = LoanPostingRequests.loanRecorded("LOAN000001", assetAccountCode, lenderContactReferenceNumber, loanAmount, loanDate)

    private fun voidedLoan() = LoanPostingRequests.loanVoided("LOAN000001", assetAccountCode, lenderContactReferenceNumber, loanAmount, voidDate)

    private fun recordedRepayment() =
        LoanPostingRequests.repaymentRecorded("LNRP000001", assetAccountCode, lenderContactReferenceNumber, repaymentAmount, repaymentDate)

    private fun voidedRepayment() =
        LoanPostingRequests.repaymentVoided("LNRP000001", assetAccountCode, lenderContactReferenceNumber, repaymentAmount, voidDate)

    private fun post(ledgerPostingRequest: LedgerPostingRequest) = ledgerPostingService.post(ledgerPostingRequest)

    private fun assertBalances(cash: String, loansPayable: String) {
        assertEquals(0, BigDecimal(cash).compareTo(recordingAccountService.balance(SystemAccount.CASH)), "cash")
        assertEquals(0, BigDecimal(loansPayable).compareTo(recordingAccountService.balance(SystemAccount.LOANS_PAYABLE)), "loans payable")
    }

    private fun assertSubledger(payable: String, receivable: String) {
        assertEquals(0, BigDecimal(payable).compareTo(subledgerBalanceEntity.runningPayable), "running payable")
        assertEquals(0, BigDecimal(receivable).compareTo(subledgerBalanceEntity.runningReceivable), "running receivable")
    }
}
