package me.ezra_home.retail_software_solution.util.business

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class PositiveMoneyTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `zero and negative amounts are rejected`() {
        listOf(BigDecimal.ZERO, BigDecimal("-1.00")).forEach { nonPositive ->
            assertThrows(RtsGenericException::class.java) { PositiveMoney.of(nonPositive) }
        }
    }

    @Test
    fun `the money rules still apply on top of positivity`() {
        val excessDecimals = assertThrows(RtsGenericException::class.java) { PositiveMoney.of(BigDecimal("1.255")) }
        assertEquals("Amount can have at most 2 decimal places", excessDecimals.message)
        assertThrows(RtsGenericException::class.java) {
            PositiveMoney.of(Decimals.MAXIMUM_TRANSACTION_AMOUNT.add(BigDecimal("0.01")))
        }
        assertEquals(Decimals.MAXIMUM_TRANSACTION_AMOUNT, PositiveMoney.of(Decimals.MAXIMUM_TRANSACTION_AMOUNT).toBigDecimal())
    }

    @Test
    fun `it travels as a bare JSON number and a non-positive number fails to bind`() {
        assertEquals("2.50", objectMapper.writeValueAsString(PositiveMoney.of(BigDecimal("2.5"))))
        assertEquals(PositiveMoney.of(BigDecimal("2.5")), objectMapper.readValue("2.5", PositiveMoney::class.java))
        val bindFailure = assertThrows(JsonMappingException::class.java) { objectMapper.readValue("0", PositiveMoney::class.java) }
        assertTrue(bindFailure.cause is RtsGenericException)
    }
}
