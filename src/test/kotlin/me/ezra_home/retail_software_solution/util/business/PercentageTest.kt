package me.ezra_home.retail_software_solution.util.business

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class PercentageTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `a percentage from zero to one hundred is accepted and normalized to two decimals`() {
        assertEquals(BigDecimal("0.00"), Percentage.of(BigDecimal.ZERO).toBigDecimal())
        assertEquals(BigDecimal("7.50"), Percentage.of(BigDecimal("7.5")).toBigDecimal())
        assertEquals(BigDecimal("100.00"), Percentage.of(BigDecimal("100")).toBigDecimal())
    }

    @Test
    fun `a negative percentage, one above one hundred and one with three decimals are rejected, never rounded`() {
        listOf("-0.01", "100.01", "7.125").forEach { rejected ->
            assertThrows(RtsGenericException::class.java) { Percentage.of(BigDecimal(rejected)) }
        }
    }

    @Test
    fun `trailing zeros beyond two decimals are not excess decimals`() {
        assertEquals(BigDecimal("7.10"), Percentage.of(BigDecimal("7.1000")).toBigDecimal())
    }

    @Test
    fun `a valid percentage binds from a bare JSON number`() {
        assertEquals(Percentage.of(BigDecimal("7.5")), objectMapper.readValue("7.5", Percentage::class.java))
    }

    @Test
    fun `a JSON percentage above one hundred fails binding with the business exception as its cause`() {
        val exception = assertThrows(JsonMappingException::class.java) { objectMapper.readValue("100.01", Percentage::class.java) }
        assertInstanceOf(RtsGenericException::class.java, exception.cause)
    }

    @Test
    fun `a JSON percentage with three decimals fails binding with the business exception as its cause`() {
        val exception = assertThrows(JsonMappingException::class.java) { objectMapper.readValue("7.125", Percentage::class.java) }
        assertInstanceOf(RtsGenericException::class.java, exception.cause)
    }
}
