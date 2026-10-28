package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@WithDefaultOrganizationSession
class LedgerEntryRequestTest {

    @Test
    fun `a zero amount leg is omitted rather than posted`() {
        assertTrue(LedgerEntryRequest.debitUnlessZero(SystemAccount.GROSS_SALES.code, BigDecimal("0.0000")).isEmpty())
        assertTrue(LedgerEntryRequest.creditUnlessZero(SystemAccount.GROSS_SALES.code, BigDecimal.ZERO).isEmpty())
    }

    @Test
    fun `a positive amount leg is built with the requested side`() {
        assertEquals(
            listOf(LedgerEntryRequest(SystemAccount.CASH.code, EntryType.DEBIT, PositiveMoney.of(BigDecimal("10.00")))),
            LedgerEntryRequest.debitUnlessZero(SystemAccount.CASH.code, BigDecimal("10.0000"))
        )
    }

    @Test
    fun `a negative amount is not skipped and fails loudly so a bad total cannot post silently`() {
        assertThrows(RtsGenericException::class.java) {
            LedgerEntryRequest.debitUnlessZero(SystemAccount.COST_OF_GOODS_SOLD.code, BigDecimal("-1.00"))
        }
    }

    @Test
    fun `an amount finer than the money scale is rejected even when the entries would balance`() {
        assertThrows(RtsGenericException::class.java) { PositiveMoney.of(BigDecimal("10.005")) }
    }
}
