package me.ezra_home.retail_software_solution.util.business

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DisplayFormattersTest {

    @Test
    fun `a quantity of exactly one is singular`() {
        assertEquals("1 bale", DisplayFormatters.pluralize("1 bale", BigDecimal.ONE))
    }

    @Test
    fun `a quantity that displays as one is singular even when the stored value sits a sliver below it`() {
        assertEquals("1 bale", DisplayFormatters.pluralize("1 bale", BigDecimal("0.9999999999")))
    }

    @Test
    fun `any other quantity is plural`() {
        assertEquals("0.5 bales", DisplayFormatters.pluralize("0.5 bale", BigDecimal("0.5")))
    }

    @Test
    fun `currency formatting rounds half up like the money scale`() {
        assertEquals("KES 0.13", DisplayFormatters.formatCurrency(BigDecimal("0.125")))
        assertEquals("KES 1,234.50", DisplayFormatters.formatCurrency(Money.of(BigDecimal("1234.5"))))
    }
}
