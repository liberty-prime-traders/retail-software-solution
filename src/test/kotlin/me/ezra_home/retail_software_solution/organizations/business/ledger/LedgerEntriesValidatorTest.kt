package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@WithDefaultOrganizationSession
class LedgerEntriesValidatorTest {

    @Test
    fun `balanced entries pass`() {
        assertDoesNotThrow {
            LedgerEntriesValidator.validate(
                listOf(
                    LedgerEntryRequest.debit(SystemAccount.CASH.code, BigDecimal("10.00")),
                    LedgerEntryRequest.credit(SystemAccount.GROSS_SALES.code, BigDecimal("10.00"))
                )
            )
        }
    }

    @Test
    fun `zero amount legs pass so fully discounted sales can post`() {
        assertDoesNotThrow {
            LedgerEntriesValidator.validate(
                listOf(
                    LedgerEntryRequest.debit(SystemAccount.TRADE_RECEIVABLES.code, BigDecimal.ZERO),
                    LedgerEntryRequest.credit(SystemAccount.GROSS_SALES.code, BigDecimal.ZERO)
                )
            )
        }
    }

    @Test
    fun `an amount finer than the money scale is rejected even when the entries balance`() {
        assertThrows(RtsGenericException::class.java) {
            LedgerEntriesValidator.validate(
                listOf(
                    LedgerEntryRequest.debit(SystemAccount.CASH.code, BigDecimal("10.005")),
                    LedgerEntryRequest.credit(SystemAccount.GROSS_SALES.code, BigDecimal("10.005"))
                )
            )
        }
    }

    @Test
    fun `trailing zeros beyond the money scale are not excess decimals`() {
        assertDoesNotThrow {
            LedgerEntriesValidator.validate(
                listOf(
                    LedgerEntryRequest.debit(SystemAccount.CASH.code, BigDecimal("10.0000")),
                    LedgerEntryRequest.credit(SystemAccount.GROSS_SALES.code, BigDecimal("10.0000"))
                )
            )
        }
    }

    @Test
    fun `unbalanced entries are rejected`() {
        assertThrows(RtsGenericException::class.java) {
            LedgerEntriesValidator.validate(
                listOf(
                    LedgerEntryRequest.debit(SystemAccount.CASH.code, BigDecimal("10.00")),
                    LedgerEntryRequest.credit(SystemAccount.GROSS_SALES.code, BigDecimal("9.00"))
                )
            )
        }
    }

    @Test
    fun `a negative amount is rejected even when the sides still balance`() {
        assertThrows(RtsGenericException::class.java) {
            LedgerEntriesValidator.validate(
                listOf(
                    LedgerEntryRequest.debit(SystemAccount.CASH.code, BigDecimal("-10.00")),
                    LedgerEntryRequest.credit(SystemAccount.GROSS_SALES.code, BigDecimal("-10.00"))
                )
            )
        }
    }
}
