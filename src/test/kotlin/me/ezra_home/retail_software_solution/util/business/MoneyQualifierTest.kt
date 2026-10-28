package me.ezra_home.retail_software_solution.util.business

import me.ezra_home.retail_software_solution.util.business.mappers.MoneyQualifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MoneyQualifierTest {

    @Test
    fun `a null amount stays null`() {
        assertNull(MoneyQualifier.toMoneyDisplay(null))
    }

    @Test
    fun `rounds half up to two decimals instead of rejecting excess decimals`() {
        assertEquals(BigDecimal("0.13"), MoneyQualifier.toMoneyDisplay(BigDecimal("0.125"))?.toBigDecimal())
        assertEquals(BigDecimal("0.12"), MoneyQualifier.toMoneyDisplay(BigDecimal("0.124"))?.toBigDecimal())
    }

    @Test
    fun `a negative amount and one above the transaction maximum are held, not rejected`() {
        assertEquals(BigDecimal("-20.00"), MoneyQualifier.toMoneyDisplay(BigDecimal("-20"))?.toBigDecimal())
        assertEquals(
            BigDecimal("1000000000000.00"),
            MoneyQualifier.toMoneyDisplay(Decimals.MAXIMUM_TRANSACTION_AMOUNT.add(BigDecimal("0.01")))?.toBigDecimal()
        )
    }
}
