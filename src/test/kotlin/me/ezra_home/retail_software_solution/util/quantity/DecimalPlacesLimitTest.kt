package me.ezra_home.retail_software_solution.util.quantity

import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DecimalPlacesLimitTest {

    private fun check(value: String) = Decimals.requireMaxDecimalPlaces(BigDecimal(value), QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES, "Quantity")

    @Test
    fun `two decimals are accepted and trailing zeros beyond them are ignored`() {
        check("1.25")
        check("1.2500")
        check("0.000")
        check("100")
        check("1E+2")
        check("-1.5")
    }

    @Test
    fun `three decimals are rejected with a business error`() {
        assertThrows(RtsGenericException::class.java) { check("1.255") }
        assertThrows(RtsGenericException::class.java) { check("0.001") }
    }
}
