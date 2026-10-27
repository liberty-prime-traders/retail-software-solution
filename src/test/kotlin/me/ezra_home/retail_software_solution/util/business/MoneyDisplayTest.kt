package me.ezra_home.retail_software_solution.util.business

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MoneyDisplayTest {

    @Test
    fun `rounds half up to the money scale instead of throwing`() {
        assertEquals(BigDecimal("0.13"), MoneyDisplay.of(BigDecimal("0.125")).toBigDecimal())
        assertEquals(BigDecimal("1.50"), MoneyDisplay.of(BigDecimal("1.5000")).toBigDecimal())
    }

    @Test
    fun `holds a negative and an amount above the transaction maximum`() {
        assertEquals(BigDecimal("-20.00"), MoneyDisplay.of(BigDecimal("-20")).toBigDecimal())
        assertEquals(
            BigDecimal("1000000000000.00"),
            MoneyDisplay.of(Decimals.MAXIMUM_TRANSACTION_AMOUNT.add(BigDecimal("0.01"))).toBigDecimal()
        )
    }

    @Test
    fun `it serializes as a bare JSON number`() {
        assertEquals("2.50", jacksonObjectMapper().writeValueAsString(MoneyDisplay.of(BigDecimal("2.5"))))
    }

    @Test
    fun `a request body cannot bind into it, so its rounding cannot be skipped`() {
        assertThrows(InvalidDefinitionException::class.java) { jacksonObjectMapper().readValue("2.555", MoneyDisplay::class.java) }
    }
}
