package me.ezra_home.retail_software_solution.organizations.business.fund_movement

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountPosting
import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class FundMovementPostingRequestsBalanceTest {

    private val recordingAccountService = RecordingAccountService()
    private val accountService = recordingAccountService.accountService

    private val movementDate = LocalDate.of(2026, 3, 1)
    private val voidDate = LocalDate.of(2026, 3, 5)
    private val amount = BigDecimal("25.0000")

    private val transfer = Movement(SystemAccount.INVENTORY, SystemAccount.CASH)
    private val capital = Movement(SystemAccount.CASH, SystemAccount.OWNERS_CAPITAL)
    private val draw = Movement(SystemAccount.OWNERS_DRAWS, SystemAccount.CASH)

    @Test
    fun `a transfer lowers the source and raises the destination`() {
        post(transfer.recorded())

        assertEquals(0, BigDecimal("-25.0000").compareTo(balance(SystemAccount.CASH)))
        assertEquals(0, BigDecimal("25.0000").compareTo(balance(SystemAccount.INVENTORY)))
    }

    @Test
    fun `a capital contribution raises the asset and owners capital`() {
        post(capital.recorded())

        assertEquals(0, BigDecimal("25.0000").compareTo(balance(SystemAccount.CASH)))
        assertEquals(0, BigDecimal("25.0000").compareTo(balance(SystemAccount.OWNERS_CAPITAL)))
    }

    @Test
    fun `a withdrawal raises owners draws, a contra with a debit normal balance, and lowers the asset`() {
        post(draw.recorded())

        assertEquals(0, BigDecimal("25.0000").compareTo(balance(SystemAccount.OWNERS_DRAWS)))
        assertEquals(0, BigDecimal("-25.0000").compareTo(balance(SystemAccount.CASH)))
    }

    @Test
    fun `voiding each type returns every account to zero, drawing owners draws back down`() {
        listOf(transfer, capital, draw).forEach { movement ->
            post(movement.recorded())
            post(movement.voided())
        }

        listOf(SystemAccount.CASH, SystemAccount.INVENTORY, SystemAccount.OWNERS_CAPITAL, SystemAccount.OWNERS_DRAWS).forEach { systemAccount ->
            assertEquals(0, BigDecimal.ZERO.compareTo(balance(systemAccount)), "${systemAccount.name} should be back to zero")
        }
    }

    @Test
    fun `a void shares its original's reference, differs by source type and posts on the void date`() {
        val recorded = draw.recorded()
        val voided = draw.voided()

        assertEquals(recorded.sourceReferenceNumber, voided.sourceReferenceNumber)
        assertEquals(LedgerSourceType.FUND_MOVEMENT, recorded.sourceType)
        assertEquals(LedgerSourceType.FUND_MOVEMENT_VOID, voided.sourceType)
        assertEquals(movementDate, recorded.postingDate)
        assertEquals(voidDate, voided.postingDate)
        assertEquals(emptyList<SubledgerEntryRequest>(), recorded.subledgerEntries)
        assertFalse(recorded.reversal)
        assertTrue(voided.reversal)
    }

    private fun post(ledgerPostingRequest: LedgerPostingRequest) {
        val accountPostings = ledgerPostingRequest.entries.map {
            AccountPosting(accountCode = it.accountCode, amount = it.amount.toBigDecimal(), entryType = it.entryType)
        }
        accountService.assertPostable(accountPostings)
        accountService.patchBalances(accountPostings)
    }

    private fun balance(systemAccount: SystemAccount) = recordingAccountService.balance(systemAccount)

    private inner class Movement(private val debit: SystemAccount, private val credit: SystemAccount) {
        fun recorded() = FundMovementPostingRequests.recorded("FMOV000001", debit.code, credit.code, amount, movementDate)
        fun voided() = FundMovementPostingRequests.voided("FMOV000001", debit.code, credit.code, amount, voidDate)
    }
}
