package me.ezra_home.retail_software_solution.util.business

import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@WithDefaultOrganizationSession
class MoneyTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `more decimals than the money scale is rejected, never rounded`() {
        val exception = assertThrows(RtsGenericException::class.java) { Money.of(BigDecimal("1.255")) }
        assertEquals("Amount can have at most 2 decimal places", exception.message)
    }

    @Test
    fun `trailing zeros beyond the money scale are accepted and are dropped`() {
        assertEquals(BigDecimal("1.25"), Money.of(BigDecimal("1.2500")).toBigDecimal())
    }

    @Test
    fun `amounts that differ only in scale are equal and hash alike`() {
        assertEquals(Money.of(BigDecimal("1.50")), Money.of(BigDecimal("1.5000")))
        assertEquals(Money.of(BigDecimal("1.50")).hashCode(), Money.of(BigDecimal("1.5000")).hashCode())
    }

    @Test
    fun `it travels as a bare JSON number in both directions`() {
        assertEquals("2.50", objectMapper.writeValueAsString(Money.of(BigDecimal("2.5"))))
        assertEquals(Money.of(BigDecimal("2.5")), objectMapper.readValue("2.5", Money::class.java))
    }

    @Test
    fun `a negative amount is rejected and zero is accepted`() {
        val exception = assertThrows(RtsGenericException::class.java) { Money.of(BigDecimal("-0.01")) }
        assertEquals("Amount must not be negative", exception.message)
        assertEquals(0, Money.of(BigDecimal.ZERO).toBigDecimal().signum())
    }

    @Test
    fun `a sum is validated against the transaction maximum like any other amount`() {
        assertEquals(Money.of(BigDecimal("3.50")), Money.of(BigDecimal("1.25")) + Money.of(BigDecimal("2.25")))
        assertThrows(RtsGenericException::class.java) {
            Money.of(Decimals.MAXIMUM_TRANSACTION_AMOUNT) + Money.of(BigDecimal("0.01"))
        }
    }

    @Test
    fun `a magnitude above the transaction maximum is rejected and the maximum itself is accepted`() {
        assertEquals(Decimals.MAXIMUM_TRANSACTION_AMOUNT, Money.of(Decimals.MAXIMUM_TRANSACTION_AMOUNT).toBigDecimal())
        val exception = assertThrows(RtsGenericException::class.java) {
            Money.of(Decimals.MAXIMUM_TRANSACTION_AMOUNT.add(BigDecimal("0.01")))
        }
        assertEquals("Amount must not exceed 999999999999.99", exception.message)
    }

    @Test
    fun `ZERO is exactly zero at the money scale`() {
        assertEquals(0, Money.ZERO.toBigDecimal().signum())
        assertEquals(2, Money.ZERO.toBigDecimal().scale())
        assertEquals(Money.of(BigDecimal.ZERO), Money.ZERO)
    }
}
