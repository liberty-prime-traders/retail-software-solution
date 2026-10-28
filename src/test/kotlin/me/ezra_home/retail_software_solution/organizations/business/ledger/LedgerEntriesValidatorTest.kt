package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
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
                    LedgerEntryRequest.debit(SystemAccount.CASH.code, PositiveMoney.of(BigDecimal("10.00"))),
                    LedgerEntryRequest.credit(SystemAccount.GROSS_SALES.code, PositiveMoney.of(BigDecimal("10.00")))
                )
            )
        }
    }

    @Test
    fun `no entries is balanced so a fully free sale still saves its group for idempotency`() {
        assertDoesNotThrow { LedgerEntriesValidator.validate(emptyList()) }
    }

    @Test
    fun `unbalanced entries are rejected`() {
        assertThrows(RtsGenericException::class.java) {
            LedgerEntriesValidator.validate(
                listOf(
                    LedgerEntryRequest.debit(SystemAccount.CASH.code, PositiveMoney.of(BigDecimal("10.00"))),
                    LedgerEntryRequest.credit(SystemAccount.GROSS_SALES.code, PositiveMoney.of(BigDecimal("9.00")))
                )
            )
        }
    }
}
