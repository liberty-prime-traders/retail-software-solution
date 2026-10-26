package me.ezra_home.retail_software_solution.util.quantity

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class PositiveUserProvidedQuantityTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `zero and negative quantities are rejected`() {
        listOf(BigDecimal.ZERO, BigDecimal("-1")).forEach { nonPositive ->
            assertThrows(RtsGenericException::class.java) { PositiveUserProvidedQuantity.of(nonPositive) }
        }
    }

    @Test
    fun `the quantity rules still apply on top of positivity`() {
        assertThrows(RtsGenericException::class.java) { PositiveUserProvidedQuantity.of(BigDecimal("1.255")) }
        assertThrows(RtsGenericException::class.java) { PositiveUserProvidedQuantity.of(QuantityLimits.MAX_USER_QUANTITY.add(BigDecimal("0.01"))) }
    }

    @Test
    fun `it travels as a bare JSON number and a non-positive number fails to bind`() {
        assertEquals("2.5", objectMapper.writeValueAsString(PositiveUserProvidedQuantity.of(BigDecimal("2.5"))))
        assertEquals(PositiveUserProvidedQuantity.of(BigDecimal("2.5")), objectMapper.readValue("2.5", PositiveUserProvidedQuantity::class.java))
        assertThrows(Exception::class.java) { objectMapper.readValue("0", PositiveUserProvidedQuantity::class.java) }
    }
}
