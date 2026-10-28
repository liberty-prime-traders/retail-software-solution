package me.ezra_home.retail_software_solution.util.quantity

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class UserProvidedQuantityTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `more than two significant decimals is rejected, never rounded`() {
        assertThrows(RtsGenericException::class.java) { UserProvidedQuantity.of(BigDecimal("1.255")) }
    }

    @Test
    fun `trailing zeros beyond two decimals are accepted and keep the typed value`() {
        assertEquals(BigDecimal("1.2500"), UserProvidedQuantity.of(BigDecimal("1.2500")).toBigDecimal())
    }

    @Test
    fun `quantities that differ only in scale are equal and hash alike`() {
        val twoDecimals = UserProvidedQuantity.of(BigDecimal("1.50"))
        val fourDecimals = UserProvidedQuantity.of(BigDecimal("1.5000"))
        assertEquals(twoDecimals, fourDecimals)
        assertEquals(twoDecimals.hashCode(), fourDecimals.hashCode())
    }

    @Test
    fun `a negative quantity is rejected and zero is accepted`() {
        val exception = assertThrows(RtsGenericException::class.java) { UserProvidedQuantity.of(BigDecimal("-0.5")) }
        assertEquals("Quantity must not be negative", exception.message)
        assertTrue(UserProvidedQuantity.of(BigDecimal("0.00")).isZero())
    }

    @Test
    fun `a magnitude above the maximum is rejected, including an enormous exponent, and the maximum itself is accepted`() {
        assertEquals(QuantityLimits.MAX_USER_QUANTITY, UserProvidedQuantity.of(QuantityLimits.MAX_USER_QUANTITY).toBigDecimal())
        listOf(QuantityLimits.MAX_USER_QUANTITY.add(BigDecimal("0.01")), BigDecimal("1e999999999")).forEach { outOfRange ->
            val exception = assertThrows(RtsGenericException::class.java) { UserProvidedQuantity.of(outOfRange) }
            assertEquals("Quantity must not exceed 999999999999.99", exception.message)
        }
    }

    @Test
    fun `it travels as a bare JSON number in both directions`() {
        assertEquals("2.5", objectMapper.writeValueAsString(UserProvidedQuantity.of(BigDecimal("2.5"))))
        assertEquals(UserProvidedQuantity.of(BigDecimal("2.5")), objectMapper.readValue("2.5", UserProvidedQuantity::class.java))
    }
}
