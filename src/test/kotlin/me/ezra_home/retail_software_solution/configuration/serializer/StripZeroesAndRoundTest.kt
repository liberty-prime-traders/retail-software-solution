package me.ezra_home.retail_software_solution.configuration.serializer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class StripZeroesAndRoundTest {

    private data class Priced(
        @field:JsonSerialize(using = StripZeroesAndRound::class)
        val price: BigDecimal?,
    )

    private val objectMapper = ObjectMapper()

    private fun serialize(price: BigDecimal?) = objectMapper.writeValueAsString(Priced(price))

    @Test
    fun `trailing zeroes of a ten decimal value are stripped`() {
        assertEquals("""{"price":10}""", serialize(BigDecimal("10.0000000000")))
        assertEquals("""{"price":10.5}""", serialize(BigDecimal("10.5000000000")))
    }

    @Test
    fun `a whole number with trailing zeroes is written plain, not in exponent form`() {
        assertEquals("""{"price":100}""", serialize(BigDecimal("100.0000000000")))
    }

    @Test
    fun `a value that is visible at four decimals is rounded half-up to four decimals`() {
        assertEquals("""{"price":10}""", serialize(BigDecimal("10.0000050000")))
        assertEquals("""{"price":0.3333}""", serialize(BigDecimal("0.3333333333")))
        assertEquals("""{"price":0.0002}""", serialize(BigDecimal("0.0001500000")))
    }

    @Test
    fun `a nonzero value that four decimals would erase keeps digits up to its first significant one`() {
        assertEquals("""{"price":0.00004}""", serialize(BigDecimal("0.0000400000")))
        assertEquals("""{"price":0.00001}""", serialize(BigDecimal("0.0000123000")))
        assertEquals("""{"price":0.00000000004}""", serialize(BigDecimal("0.0000000000400000")))
        assertEquals("""{"price":-0.00004}""", serialize(BigDecimal("-0.0000400000")))
    }

    @Test
    fun `zero is written as zero`() {
        assertEquals("""{"price":0}""", serialize(BigDecimal("0.0000000000")))
    }

    @Test
    fun `a null value stays null`() {
        assertEquals("""{"price":null}""", serialize(null))
    }
}
