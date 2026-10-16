package me.ezra_home.retail_software_solution.util.business

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DecimalsTest {

    @Test
    fun `values equal at scale 4 compare equal whatever their stored scale`() {
        assertEquals(0, Decimals.Scale4.compare(BigDecimal("2080"), BigDecimal("2080.0000")))
        assertEquals(0, Decimals.Scale4.compare(BigDecimal("25.99996"), BigDecimal("26.0000")))
    }

    @Test
    fun `values that differ at scale 4 are ordered`() {
        assertTrue(Decimals.Scale4.compare(BigDecimal("25.9992"), BigDecimal("26")) < 0)
        assertTrue(Decimals.Scale4.compare(BigDecimal("26.0001"), BigDecimal("26")) > 0)
    }

    @Test
    fun `the scale overload compares at the scale it is given`() {
        assertEquals(0, Decimals.compareAtScale(BigDecimal("25.9992"), BigDecimal("26"), 2))
        assertTrue(Decimals.compareAtScale(BigDecimal("25.9992"), BigDecimal("26"), 4) < 0)
    }
}
