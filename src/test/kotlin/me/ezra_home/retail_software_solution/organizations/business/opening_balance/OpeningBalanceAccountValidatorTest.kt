package me.ezra_home.retail_software_solution.organizations.business.opening_balance

import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpeningBalanceAccountValidatorTest {

    @Test
    fun `trade receivables and trade payables cannot take an opening balance`() {
        assertThrows(RtsGenericException::class.java) {
            OpeningBalanceAccountValidator.assertManualOpeningBalanceAccepted(SystemAccount.TRADE_RECEIVABLES.code)
        }
        assertThrows(RtsGenericException::class.java) {
            OpeningBalanceAccountValidator.assertManualOpeningBalanceAccepted(SystemAccount.TRADE_PAYABLES.code)
        }
    }

    @Test
    fun `loans payable cannot take an opening balance and the message says it is driven by loans`() {
        val exception = assertThrows(RtsGenericException::class.java) {
            OpeningBalanceAccountValidator.assertManualOpeningBalanceAccepted(SystemAccount.LOANS_PAYABLE.code)
        }

        assertTrue(exception.message.contains("loans"), exception.message)
        assertFalse(exception.message.contains("contact opening"), exception.message)
    }

    @Test
    fun `other accounts are not blocked`() {
        OpeningBalanceAccountValidator.assertManualOpeningBalanceAccepted(SystemAccount.WAGES_PAYABLE.code)
    }
}
